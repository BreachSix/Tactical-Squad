package com.breachsixgames.tacticalsquad

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsService
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import com.google.android.gms.ads.MobileAds
import org.json.JSONObject

class TwaLauncherActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TwaLauncherActivity"
        private const val LAUNCH_URL = "https://breachsixgames.com/"
        private const val POST_MESSAGE_ORIGIN = "https://breachsixgames.com"
        private const val CHANNEL_TIMEOUT_MS = 2000L
    }

    private var customTabsSession: CustomTabsSession? = null
    private var serviceConnection: CustomTabsServiceConnection? = null
    private var twaLaunched = false

    // Le canal postMessage n'est demandé qu'une fois les DEUX conditions
    // réunies : la navigation vers LAUNCH_URL est terminée, ET Chrome a
    // confirmé (via Digital Asset Links) que cette appli a le droit de
    // s'identifier comme POST_MESSAGE_ORIGIN — deux DAL distincts sont en
    // jeu ici, jamais interchangeables : handle_all_urls autorise juste
    // l'affichage plein écran (déjà en place), tandis que use_as_origin
    // est celui qui autorise réellement le canal postMessage. Sans lui,
    // Chrome refuse le canal en silence : onMessageChannelReady ne se
    // déclenche jamais, sans la moindre erreur visible.
    private var navigationFinished = false
    private var originValidated = false
    private var postMessageChannelRequested = false

    private lateinit var adManager: RewardedAdManager

    private val customTabsCallback = object : CustomTabsCallback() {
        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
            if (navigationEvent == NAVIGATION_FINISHED) {
                navigationFinished = true
                maybeRequestPostMessageChannel()
            }
        }

        override fun onRelationshipValidationResult(
            relation: Int,
            requestedOrigin: Uri,
            result: Boolean,
            extras: Bundle?
        ) {
            Log.d(TAG, "validateRelationship relation=$relation origin=$requestedOrigin résultat=$result")
            if (relation == CustomTabsService.RELATION_USE_AS_ORIGIN) {
                originValidated = result
                maybeRequestPostMessageChannel()
            }
        }

        override fun onMessageChannelReady(extras: Bundle?) {
            Log.d(TAG, "postMessage channel ready")
            // Premier postMessage = poignée de main. C'est ce message qui fait
            // que Chrome transmet enfin le MessagePort au JS (event.ports[0]).
            customTabsSession?.postMessage("{\"type\":\"NATIVE_BRIDGE_READY\"}", null)
        }

        override fun onPostMessage(message: String, extras: Bundle?) {
            handleIncomingMessage(message)
        }
    }

    private fun maybeRequestPostMessageChannel() {
        if (navigationFinished && originValidated && !postMessageChannelRequested) {
            postMessageChannelRequested = true
            val requested = customTabsSession
                ?.requestPostMessageChannel(Uri.parse(POST_MESSAGE_ORIGIN))
            Log.d(TAG, "requestPostMessageChannel (navigation finie + origine validée): $requested")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this)
        adManager = RewardedAdManager(this) { success -> sendAdResult(success) }
        adManager.preload()

        bindCustomTabsService()

        window.decorView.postDelayed({ launchTwa() }, CHANNEL_TIMEOUT_MS)
    }

    private fun bindCustomTabsService() {
        val packageName = CustomTabsClient.getPackageName(this, null)
        if (packageName == null) {
            Log.w(TAG, "Aucun provider Custom Tabs disponible sur l'appareil")
            return
        }

        val connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0)
                val session = client.newSession(customTabsCallback)
                if (session == null) {
                    Log.w(TAG, "Impossible de créer une session Custom Tabs")
                    return
                }
                customTabsSession = session
                // DAL n°1 : autorise l'affichage plein écran de la TWA sur ce domaine.
                session.validateRelationship(
                    CustomTabsService.RELATION_HANDLE_ALL_URLS,
                    Uri.parse(LAUNCH_URL),
                    null
                )
                // DAL n°2 (distinct du précédent) : autorise cette appli à
                // s'identifier comme POST_MESSAGE_ORIGIN dans le canal
                // postMessage — condition requise avant tout
                // requestPostMessageChannel (voir maybeRequestPostMessageChannel).
                session.validateRelationship(
                    CustomTabsService.RELATION_USE_AS_ORIGIN,
                    Uri.parse(POST_MESSAGE_ORIGIN),
                    null
                )
                launchTwa()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                customTabsSession = null
            }
        }
        serviceConnection = connection
        CustomTabsClient.bindCustomTabsService(this, packageName, connection)
    }

    private fun launchTwa() {
        if (twaLaunched) return
        twaLaunched = true
        val session = customTabsSession
        if (session != null) {
            val twaIntent = TrustedWebActivityIntentBuilder(Uri.parse(LAUNCH_URL)).build(session)
            twaIntent.launchTrustedWebActivity(this)
        } else {
            val fallbackIntent = androidx.browser.customtabs.CustomTabsIntent.Builder().build()
            fallbackIntent.launchUrl(this, Uri.parse(LAUNCH_URL))
        }
    }


    override fun onDestroy() {
        serviceConnection?.let { unbindService(it) }
        super.onDestroy()
    }

    private fun handleIncomingMessage(message: String) {
        val json = try {
            JSONObject(message)
        } catch (e: Exception) {
            Log.w(TAG, "Message web illisible: $message")
            return
        }
        when (json.optString("type")) {
            "REQUEST_REWARDED_AD" -> adManager.requestAd()
            else -> Log.d(TAG, "Message web ignoré (type inconnu): $message")
        }
    }

    private fun sendAdResult(success: Boolean) {
        val session = customTabsSession
        if (session == null) {
            Log.w(TAG, "Pas de session postMessage active, impossible d'envoyer REWARDED_AD_RESULT")
            return
        }
        val payload = JSONObject()
            .put("type", "REWARDED_AD_RESULT")
            .put("success", success)
            .toString()
        session.postMessage(payload, null)
    }
}
