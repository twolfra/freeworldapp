import { useEffect, useState } from 'react';
import { auth, messages as messagesApi } from '../api/client';
import { t, getLang, setLang } from '../i18n';
import styles from './Navbar.module.css';

export default function Navbar() {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  const [unread, setUnread] = useState(0);
  const lang = getLang();

  useEffect(() => {
    if (!currentUser?.token) return;

    const refreshCount = () => {
      messagesApi.getUnreadCount(currentUser.id)
        .then((r) => setUnread(r.count))
        .catch(() => {});
    };

    refreshCount();

    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const ws = new WebSocket(
      `${proto}://${window.location.host}/ws/messages?userId=${currentUser.id}&token=${currentUser.token}`
    );
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'message' || data.type === 'read') refreshCount();
    };

    return () => ws.close();
  }, [currentUser?.id]);

  async function signOut() {
    await auth.logout().catch(() => {});
    localStorage.removeItem('currentUser');
    window.location.href = '/';
  }

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <a href="/" className={styles.brand}>
          <span className={styles.logoText}>Free<span>World</span></span>
        </a>

        <nav className={styles.nav}>
          <a href="/offers" className={styles.navLink}>{t('nav.offers')}</a>
          <a href="/requests" className={styles.navLink}>{t('nav.requests')}</a>
          {currentUser && (
            <a href="/messages" className={styles.navLink}>
              {t('nav.messages')}
              {unread > 0 && <span className={styles.badge}>{unread > 99 ? '99+' : unread}</span>}
            </a>
          )}
          {currentUser && <a href="/subscriptions" className={styles.navLink}>{t('nav.following')}</a>}
          {currentUser && <a href="/likes" className={styles.navLink}>{t('nav.likes')}</a>}
        </nav>

        <div className={styles.actions}>
          <button
            className={styles.langToggle}
            onClick={() => setLang(lang === 'de' ? 'en' : 'de')}
            title={lang === 'de' ? 'Switch to English' : 'Auf Deutsch wechseln'}
          >
            {lang === 'de' ? 'EN' : 'DE'}
          </button>
          {currentUser ? (
            <>
              <a href={`/users/${currentUser.id}`} className={styles.userChip}>
                <span className={styles.avatar}>{currentUser.username.charAt(0).toUpperCase()}</span>
                <span className={styles.userName}>{currentUser.username}</span>
              </a>
              <button className={styles.signOut} onClick={signOut} title={t('nav.signOut')}>{t('nav.signOut')}</button>
            </>
          ) : (
            <>
              <a href="/login" className={styles.navLink}>{t('nav.signIn')}</a>
              <a href="/register" className="btn-secondary">{t('nav.join')}</a>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
