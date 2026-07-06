import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import useUnreadCount from '../hooks/useUnreadCount';
import { t, getLang, setLang } from '../i18n';
import { resolvedTheme, setTheme } from '../theme';
import styles from './Navbar.module.css';

export default function Navbar() {
  const { user: currentUser, logout } = useAuth();
  const navigate = useNavigate();
  const { unread, notifUnread } = useUnreadCount(currentUser, { withNotifications: true });
  const [theme, setThemeState] = useState(resolvedTheme);
  const lang = getLang();

  function toggleTheme() {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    setThemeState(next);
  }

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
          {currentUser && (
            <Link to="/notifications" className={styles.navLink} aria-label={t('nav.notifications')} title={t('nav.notifications')}>
              🔔
              {notifUnread > 0 && <span className={styles.badge}>{notifUnread > 99 ? '99+' : notifUnread}</span>}
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
            className={styles.themeToggle}
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? t('nav.themeLight') : t('nav.themeDark')}
            title={theme === 'dark' ? t('nav.themeLight') : t('nav.themeDark')}
          >
            <span aria-hidden="true">{theme === 'dark' ? '☀️' : '🌙'}</span>
          </button>
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
                {currentUser.avatarUrl
                  ? <img src={currentUser.avatarUrl} alt="" className={styles.avatarImg} />
                  : <span className={styles.avatar}>{currentUser.username.charAt(0).toUpperCase()}</span>}
                <span className={styles.userName}>{currentUser.username}</span>
              </Link>
              <Link
                to="/settings"
                className={styles.settingsLink}
                title={t('nav.settings')}
                aria-label={t('nav.settings')}
              >
                <span aria-hidden="true">⚙️</span>
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
