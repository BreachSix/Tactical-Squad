const CACHE_VERSION = 'v104';
const CACHE_NAME = 'breachsix-' + CACHE_VERSION;
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png',
  './sound-assault.mp3',
  './sound-heavy.mp3',
  './sound-light.mp3',
  './sound-night.mp3',
  './sound-explosion.mp3',
  './sound-mission-stinger.mp3',
  './sound-night-vision.mp3',
  './sound-bomb-tick.mp3',
  './sound-shadow-blade.mp3',
  './sound-unlock.mp3',
  './sound-click.mp3',
  './music-menu.mp3',
  './brief-residence.jpg',
  './brief-entrepot.jpg',
  './brief-bureaux.jpg',
  './brief-banque.jpg',
  './brief-desert.jpg',
  './brief-jungle.jpg',
  './brief-arctique.jpg',
  './brief-hotel.jpg',
  './brief-station.jpg',
  './brief-train.jpg',
  './brief-cargo.jpg',
  './brief-hangar.jpg',
  './op-urbain.png',
  './op-desert.png',
  './op-jungle.png',
  './op-arctique.png',
  './op-urbain-cuivre.png',
  './op-urbain-argent.png',
  './op-urbain-or.png',
  './op-urbain-damas.png',
  './op-urbain-platine.png',
  './op-urbain-diamant.png',
  './op-desert-cuivre.png',
  './op-desert-argent.png',
  './op-desert-or.png',
  './op-desert-damas.png',
  './op-desert-platine.png',
  './op-desert-diamant.png',
  './op-jungle-cuivre.png',
  './op-jungle-argent.png',
  './op-jungle-or.png',
  './op-jungle-damas.png',
  './op-jungle-platine.png',
  './op-jungle-diamant.png',
  './op-arctique-cuivre.png',
  './op-arctique-argent.png',
  './op-arctique-or.png',
  './op-arctique-damas.png',
  './op-arctique-platine.png',
  './op-arctique-diamant.png',
  './op-heavy-urbain.png',
  './op-heavy-desert.png',
  './op-heavy-jungle.png',
  './op-heavy-arctique.png',
  './op-light-urbain.png',
  './op-light-desert.png',
  './op-light-jungle.png',
  './op-light-arctique.png',
  './op-shadow.png',
  './enemy-urbain.png',
  './enemy-desert.png',
  './enemy-jungle.png',
  './enemy-arctique.png',
  './faction-logo.jpg',
  './unit-logo.jpg',
  './skin-cuivre.jpg',
  './skin-argent.jpg',
  './skin-or.jpg',
  './skin-damas.jpg',
  './skin-platine.jpg',
  './skin-diamant.jpg',
  './milestone-tier0-standard.jpg',
  './milestone-tier1-acier-brosse.jpg',
  './milestone-tier2-noir-tactique.jpg',
  './milestone-tier3-cuivre.jpg',
  './milestone-tier4-bronze-antique.jpg',
  './milestone-tier5-argent.jpg',
  './milestone-tier6-or.jpg',
  './milestone-tier7-nacre.jpg',
  './milestone-tier8-platine.jpg',
  './milestone-tier9-diamant-taille.jpg',
  './milestone-tier10-damascus-arc-en-ciel.jpg',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  const isHtml = req.mode === 'navigate' ||
    (req.method === 'GET' && req.headers.get('accept') && req.headers.get('accept').includes('text/html'));

  if (isHtml) {
    // Réseau d'abord : charge toujours la version la plus récente du jeu si
    // une connexion est disponible. En cas d'échec (hors ligne), se rabat
    // sur la dernière version mise en cache.
    event.respondWith(
      fetch(req)
        .then((res) => {
          const resClone = res.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(req, resClone));
          return res;
        })
        .catch(() =>
          caches.match(req).then((cached) => cached || caches.match('./index.html'))
        )
    );
    return;
  }

  // Cache d'abord pour le reste (icônes, manifest) — ces fichiers changent
  // rarement, autant les servir instantanément depuis le cache.
  event.respondWith(
    caches.match(req).then((cached) => cached || fetch(req).catch(() => cached))
  );
});
