import { useEffect, useState } from 'react';
import { t } from '../i18n';
import styles from './InstallPrompt.module.css';

const DISMISS_KEY = 'fw_install_dismissed';

/**
 * Subtle "Install app" hint (AP 4.1). Chromium fires `beforeinstallprompt`
 * when the PWA is installable; we stash the event and show a dismissible
 * banner that triggers the native prompt. Dismissal is remembered in
 * localStorage so the banner never nags.
 */
export default function InstallPrompt() {
  const [promptEvent, setPromptEvent] = useState(null);

  useEffect(() => {
    function onBeforeInstallPrompt(e) {
      // Take over the mini-infobar; we show our own hint instead.
      e.preventDefault();
      if (localStorage.getItem(DISMISS_KEY)) return;
      setPromptEvent(e);
    }
    function onInstalled() {
      setPromptEvent(null);
    }
    window.addEventListener('beforeinstallprompt', onBeforeInstallPrompt);
    window.addEventListener('appinstalled', onInstalled);
    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstallPrompt);
      window.removeEventListener('appinstalled', onInstalled);
    };
  }, []);

  if (!promptEvent) return null;

  async function install() {
    promptEvent.prompt();
    await promptEvent.userChoice.catch(() => {});
    setPromptEvent(null);
  }

  function dismiss() {
    localStorage.setItem(DISMISS_KEY, '1');
    setPromptEvent(null);
  }

  return (
    <div className={styles.banner} role="region" aria-label={t('pwa.install')}>
      <span className={styles.text}>{t('pwa.installText')}</span>
      <button type="button" className={styles.installBtn} onClick={install}>
        {t('pwa.install')}
      </button>
      <button type="button" className={styles.dismiss} onClick={dismiss} aria-label={t('ui.dismiss')}>
        <span aria-hidden="true">✕</span>
      </button>
    </div>
  );
}
