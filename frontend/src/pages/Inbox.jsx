import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { messages as messagesApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t } from '../i18n';
import styles from './Inbox.module.css';

export default function Inbox() {
  const { user: currentUser } = useAuth();
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!currentUser) { setLoading(false); return; }
    messagesApi.getConversations(currentUser.id)
      .then(setConversations)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (!currentUser) return (
    <main className={styles.page}>
      <p className={styles.status}>
        Please <Link to="/login">{t('inbox.signInLink')}</Link> {t('inbox.signIn').replace('Please {link} ', '')}
      </p>
    </main>
  );

  if (loading) return <p className={styles.status}>{t('detail.loading')}</p>;
  if (error)   return <p className={styles.status}>Error: {error}</p>;

  return (
    <main className={styles.page}>
      <div className={styles.header}>
        <h2>{t('inbox.title')}</h2>
      </div>
      {conversations.length === 0 ? (
        <p className={styles.empty}>{t('inbox.empty')}</p>
      ) : (
        <ul className={styles.list}>
          {conversations.map((c) => (
            <li key={c.userId}>
              <Link to={`/messages/${c.userId}`} className={`${styles.row} ${c.unreadCount > 0 ? styles.unread : ''}`}>
                <div className={styles.avatar}>{c.username[0].toUpperCase()}</div>
                <div className={styles.info}>
                  <span className={styles.username}>{c.username}</span>
                  <span className={styles.preview}>{c.lastMessage}</span>
                </div>
                <div className={styles.meta}>
                  <time className={styles.time}>
                    {new Date(c.lastMessageAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                  </time>
                  {c.unreadCount > 0 && (
                    <span className={styles.unreadBadge}>{c.unreadCount > 99 ? '99+' : c.unreadCount}</span>
                  )}
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
