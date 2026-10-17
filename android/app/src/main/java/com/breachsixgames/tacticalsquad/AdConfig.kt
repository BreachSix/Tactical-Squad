package com.breachsixgames.tacticalsquad

/**
 * Identifiants AdMob et interrupteur test/production.
 *
 * IMPORTANT — ne jamais tester avec les identifiants de production : cliquer
 * sur ses propres pubs réelles pendant le développement expose le compte
 * AdMob à une suspension pour trafic invalide. Laisser USE_TEST_ADS = true
 * pendant tout le développement, et le repasser à false seulement juste
 * avant la publication finale sur Google Play (voir la checklist en bas de
 * ce fichier avant de basculer).
 */
object AdConfig {

    /** true = identifiants de test Google (safe à cliquer autant que voulu). */
    const val USE_TEST_ADS = false

    // --- Production (compte AdMob breachsixgames@gmail.com) ---
    private const val PROD_APP_ID = "ca-app-pub-5775885539187715~3715956057"
    private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-5775885539187715/1621199195"

    // --- Test (identifiants officiels Google, communs à tous les développeurs) ---
    private const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    val appId: String
        get() = if (USE_TEST_ADS) TEST_APP_ID else PROD_APP_ID

    val rewardedAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
}

/*
 * Checklist avant de passer USE_TEST_ADS à false (voir cahier des charges) :
 *  - exactement un REWARDED_AD_RESULT par REQUEST_REWARDED_AD, jamais zéro, jamais deux
 *  - testé sur un appareil Android réel, pas seulement un émulateur
 *  - le jeu retombe bien sur la pub simulée si le pont natif n'est pas détecté
 *    (comportement déjà en place côté web — ne pas casser)
 *  - success: false correctement envoyé si le joueur ferme la pub avant la fin
 *
 * L'App ID (AdConfig.appId) doit aussi être mis à jour dans
 * AndroidManifest.xml (meta-data com.google.android.gms.ads.APPLICATION_ID) —
 * ce champ est lu par le SDK avant même que ce fichier Kotlin ne s'exécute,
 * donc il ne peut pas être piloté par USE_TEST_ADS. Utiliser l'App ID de
 * test dans le manifeste pendant tout le développement, production seulement
 * juste avant publication.
 */
