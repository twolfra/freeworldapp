import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { messages as messagesApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t, getLang, setLang } from '../i18n';
import styles from './Navbar.module.css';

export default function Navbar() {
  const { user: currentUser, logout } = useAuth();
  const navigate = useNavigate();
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
      `${proto}://${window.location.host}/ws/messages`
    );
    // First frame must authenticate — the token no longer travels in the URL.
    ws.onopen = () => ws.send(JSON.stringify({ type: 'auth', token: currentUser.token }));
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'message' || data.type === 'read') refreshCount();
    };

    return () => ws.close();
  }, [currentUser?.id]);

  async function signOut() {
    await logout();
    navigate('/');
  }

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link to="/" className={styles.brand}>
          <span className={styles.logoText}>Free<span>World</span></span>
        </Link>

        <nav className={styles.nav}>
          {currentUser && (
            <Link to="/messages" className={styles.navLink}>
              {t('nav.messages')}
              {unread > 0 && <span className={styles.badge}>{unread > 99 ? '99+' : unread}</span>}
            </Link>
          )}
          {currentUser && <Link to="/subscriptions" className={styles.navLink}>{t('nav.following')}</Link>}
          {currentUser && <Link to="/likes" className={styles.navLink}>{t('nav.likes')}</Link>}
          {currentUser?.role === 'ADMIN' && (
            <Link to="/admin" className={styles.navLink}>{t('nav.admin')}</Link>
          )}
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
              <Link to={`/users/${currentUser.id}`} className={styles.userChip}>
                <span className={styles.avatar}>{currentUser.username.charAt(0).toUpperCase()}</span>
                <span className={styles.userName}>{currentUser.username}</span>
              </Link>
              <button className={styles.signOut} onClick={signOut} title={t('nav.signOut')}>{t('nav.signOut')}</button>
            </>
          ) : (
            <>
              <Link to="/login" className={styles.navLink}>{t('nav.signIn')}</Link>
              <Link to="/register" className="btn-secondary">{t('nav.join')}</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
