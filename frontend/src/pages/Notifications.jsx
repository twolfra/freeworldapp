import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { notifications as notificationsApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t, tp } from '../i18n';
import { Button, EmptyState, Skeleton, useToast } from '../components/ui';
import styles from './Notifications.module.css';

const ICONS = {
  NEW_MESSAGE: '💬',
  NEW_POST_FROM_SUB: '📣',
  INTEREST: '🎁',
  THANKS: '💚',
  ADMIN_NOTICE: '📢',
};

function sentence(n) {
  const p = n.payload || {};
  switch (n.type) {
    case 'NEW_MESSAGE':       return tp('notif.newMessage', { user: p.fromUsername });
    case 'NEW_POST_FROM_SUB': return p.postType === 'REQUEST'
        ? tp('notif.newPostRequest', { user: p.username, title: p.title })
        : tp('notif.newPostOffer', { user: p.username, title: p.title });
    case 'INTEREST':          return tp('notif.interest', { user: p.fromUsername, title: p.offerTitle });
    case 'THANKS':            return tp('notif.thanks', { user: p.fromUsername, title: p.offerTitle });
    default:                  return t('notif.generic');
  }
}

function target(n) {
  const p = n.payload || {};
  switch (n.type) {
    case 'NEW_MESSAGE':       return `/messages/${p.fromId}`;
    case 'NEW_POST_FROM_SUB': return `/${p.postType === 'REQUEST' ? 'requests' : 'offers'}/${p.postId}`;
    case 'INTEREST':
    case 'THANKS':            return `/offers/${p.offerId}`;
    default:                  return '/notifications';
  }
}

function relativeTime(iso) {
  const diffMin = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
  if (diffMin < 1) return t('time.justNow');
  if (diffMin < 60) return tp('time.minAgo', { n: diffMin });
  const diffH = Math.floor(diffMin / 60);
  if (diffH < 24) return tp('time.hoursAgo', { n: diffH });
  return tp('time.daysAgo', { n: Math.floor(diffH / 24) });
}

export default function Notifications() {
  const { user: currentUser } = useAuth();
  const toast = useToast();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [marking, setMarking] = useState(false);

  useEffect(() => {
    if (!currentUser) return;
    notificationsApi.list().then(setData).catch((e) => setError(e.message));
  }, [currentUser]);

  if (!currentUser) return (
    <main className={styles.page}>
      <p className={styles.status}>
        <Link to="/login">{t('conv.signInLink')}</Link>
      </p>
    </main>
  );

  async function markAllRead() {
    setMarking(true);
    try {
      await notificationsApi.markAllRead();
      setData((d) => d && {
        unread: 0,
        items: d.items.map((i) => ({ ...i, readAt: i.readAt || new Date().toISOString() })),
      });
      // tells the Navbar hook to refresh its bell badge
      window.dispatchEvent(new Event('fw:notifications-read'));
    } catch (e) {
      toast.error(e.message);
    } finally {
      setMarking(false);
    }
  }

  return (
    <main className={styles.page}>
      <div className={styles.head}>
        <h1>{t('notif.heading')}</h1>
        {data && data.unread > 0 && (
          <Button variant="secondary" size="sm" loading={marking} onClick={markAllRead}>
            {t('notif.markAllRead')}
          </Button>
        )}
      </div>

      {error && <p className={styles.status}>{error}</p>}
      {!data && !error && (
        <div className={styles.list}>
          {[...Array(4)].map((_, i) => <Skeleton key={i} height={64} />)}
        </div>
      )}

      {data && data.items.length === 0 && (
        <EmptyState icon="🔔" title={t('notif.emptyTitle')} text={t('notif.emptyText')} />
      )}

      {data && data.items.length > 0 && (
        <ul className={styles.list}>
          {data.items.map((n) => (
            <li key={n.id} className={n.readAt ? styles.item : styles.itemUnread}>
              <Link to={target(n)} className={styles.itemLink}>
                <span className={styles.icon} aria-hidden="true">{ICONS[n.type] || '🔔'}</span>
                <span className={styles.text}>
                  {sentence(n)}
                  <span className={styles.time}>{relativeTime(n.createdAt)}</span>
                </span>
                {!n.readAt && <span className={styles.dot} aria-label={t('notif.unread')} />}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
