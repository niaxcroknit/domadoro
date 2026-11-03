// 도마도로 웹: 한 번 열면 인터넷 없이도 열리도록 저장해 둬요.
const CACHE = 'domadoro-v2';
const FILES = ['./', './index.html', './manifest.webmanifest', './play-store/icon-512.png'];
self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES))); self.skipWaiting(); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k))))); self.clients.claim(); });
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  // 새 버전을 먼저 받고, 안 되면 저장해 둔 걸 보여줘요.
  e.respondWith(fetch(e.request).then(r => { const copy = r.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); return r; }).catch(() => caches.match(e.request)));
});
