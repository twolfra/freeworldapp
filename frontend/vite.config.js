import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    react(),
    // AP 4.1 — PWA: app-shell precache + manifest + runtime caching.
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'icons.svg', 'icons/icon.svg'],
      manifest: {
        name: 'FreeWorld',
        short_name: 'FreeWorld',
        description:
          'Deine Community für eine Schenkökonomie — verschenken, finden, teilen. Keine Preise, kein Tausch, kein Geld.',
        lang: 'de',
        start_url: '/',
        display: 'standalone',
        theme_color: '#1f7a3d',      // --green (index.css)
        background_color: '#f2f3f5', // --bg, light theme (index.css)
        icons: [
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: '/icons/icon-512-maskable.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
          { src: '/icons/icon.svg', sizes: 'any', type: 'image/svg+xml' },
        ],
      },
      workbox: {
        // Offline strategy: the SPA shell (index.html + JS/CSS) is precached,
        // so navigations work offline and pages show their normal error/empty
        // states when API calls fail — no separate offline.html needed.
        navigateFallback: '/index.html',
        // API and WebSocket paths must never be answered with the app shell.
        navigateFallbackDenylist: [/^\/api\//, /^\/ws/],
        runtimeCaching: [
          {
            // Uploaded images served by the backend (dev/local storage).
            urlPattern: ({ url, sameOrigin }) => sameOrigin && url.pathname.startsWith('/api/images/'),
            handler: 'StaleWhileRevalidate',
            options: {
              cacheName: 'api-images',
              expiration: { maxEntries: 60, maxAgeSeconds: 30 * 24 * 60 * 60 },
            },
          },
          {
            // Production images on GCS.
            urlPattern: /^https:\/\/storage\.googleapis\.com\/.*/i,
            handler: 'StaleWhileRevalidate',
            options: {
              cacheName: 'gcs-images',
              cacheableResponse: { statuses: [0, 200] },
              expiration: { maxEntries: 60, maxAgeSeconds: 30 * 24 * 60 * 60 },
            },
          },
          {
            // OpenStreetMap tiles for the map view.
            urlPattern: /^https:\/\/[abc]\.tile\.openstreetmap\.org\/.*/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'osm-tiles',
              cacheableResponse: { statuses: [0, 200] },
              expiration: { maxEntries: 100, maxAgeSeconds: 7 * 24 * 60 * 60 },
            },
          },
          {
            // Everything else under /api — network first with a short timeout
            // so stale data appears quickly when offline/flaky. Runtime caching
            // only ever applies to GET requests (workbox default), so mutations
            // are never cached.
            urlPattern: ({ url, sameOrigin }) => sameOrigin && url.pathname.startsWith('/api/'),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api',
              networkTimeoutSeconds: 4,
              expiration: { maxEntries: 50, maxAgeSeconds: 5 * 60 },
            },
          },
        ],
      },
    }),
  ],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
})
