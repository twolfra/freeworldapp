import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { registerSW } from 'virtual:pwa-register'
import './index.css'
import App from './App.jsx'
import { applyTheme } from './theme'
import { getLang } from './i18n'

// Resolve light/dark before the first paint to avoid a theme flash.
applyTheme()

// <html lang> must follow the i18n language (AP 4.2); setLang() keeps it in
// sync on switches (the page reloads, so this line runs again anyway).
document.documentElement.lang = getLang()

// PWA service worker (AP 4.1): precaches the app shell, auto-updates on new
// deploys (registerType 'autoUpdate' in vite.config.js). No-op in dev.
registerSW({ immediate: true })

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
