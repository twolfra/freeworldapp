import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import useUnreadCount from '../hooks/useUnreadCount';
import { t } from '../i18n';
import styles from './TabBar.module.css';

// Mobile-only bottom tab bar (hidden ≥768px via CSS media query).
// Desktop keeps the regular Navbar; below 768px the Navbar's nav links
// are hidden and this bar takes over primary navigation.
export default function TabBar() {
  const { user: currentUser } = useAuth();
  const { unread } = useUnreadCount(currentUser);

  const linkClass = ({ isActive }) =>
    isActive ? `${styles.tab} ${styles.active}` : styles.tab;

  return (
    <nav className={styles.tabBar} aria-label={t('nav.tabBar')}>
      <NavLink to="/" end className={linkClass}>
        <span className={styles.icon} aria-hidden="true">🏠</span>
        <span className={styles.label}>{t('nav.discover')}</span>
      </NavLink>

      <NavLink to="/search" className={linkClass}>
        <span className={styles.icon} aria-hidden="true">🔍</span>
        <span className={styles.label}>{t('nav.search')}</span>
      </NavLink>

      <NavLink
        to="/offers/new"
        className={({ isActive }) =>
          isActive ? `${styles.tab} ${styles.give} ${styles.active}` : `${styles.tab} ${styles.give}`
        }
      >
        <span className={styles.giveIcon} aria-hidden="true">＋</span>
        <span className={styles.label}>{t('nav.give')}</span>
      </NavLink>

      <NavLink to="/messages" className={linkClass}>
        <span className={styles.iconWrap}>
          <span className={styles.icon} aria-hidden="true">💬</span>
          {unread > 0 && (
            <span className={styles.badge}>{unread > 99 ? '99+' : unread}</span>
          )}
        </span>
        <span className={styles.label}>{t('nav.messages')}</span>
      </NavLink>

      <NavLink
        to={currentUser ? `/users/${currentUser.id}` : '/login'}
        className={linkClass}
      >
        <span className={styles.icon} aria-hidden="true">👤</span>
        <span className={styles.label}>{t('nav.profile')}</span>
      </NavLink>
    </nav>
  );
}
