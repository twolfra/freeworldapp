import { useEffect, useState } from 'react';
import { subscriptions as subsApi } from '../api/client';
import styles from './Subscriptions.module.css';

export default function Subscriptions() {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  const [feed, setFeed]       = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  useEffect(() => {
    if (!currentUser) { setLoading(false); return; }
    subsApi.feed(currentUser.id)
      .then(setFeed)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (!currentUser) return (
    <main className={styles.page}>
      <p className={styles.status}>Please <a href="/login">sign in</a> to see your subscriptions feed.</p>
    </main>
  );

  if (loading) return <p className={styles.status}>Loading…</p>;
  if (error)   return <p className={styles.status}>Could not load feed: {error}</p>;

  return (
    <main className={styles.page}>
      <div className={styles.header}>
        <h2>Subscriptions</h2>
      </div>
      {feed.length === 0
        ? <p className={styles.empty}>Nothing here yet. <a href="/offers">Browse offers</a> or <a href="/requests">requests</a> and subscribe to users you want to follow.</p>
        : <ul className={styles.feed}>
            {feed.map((item) => (
              <li key={`${item.type}-${item.id}`}>
                <a href={`/${item.type}s/${item.id}`} className={styles.card}>
                  <div className={styles.cardTop}>
                    <span className={`${styles.badge} ${item.type === 'offer' ? styles.badgeOffer : styles.badgeRequest}`}>
                      {item.type === 'offer' ? 'Offer' : 'Request'}
                    </span>
                    <span className={styles.category}>{item.category}</span>
                    <span className={styles.meta}>
                      by <a href={`/users/${item.authorId}`} className={styles.authorLink} onClick={(e) => e.stopPropagation()}>@{item.authorUsername}</a>
                      {' · '}
                      {new Date(item.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                    </span>
                  </div>
                  <h3>{item.title}</h3>
                  <p>{item.description}</p>
                  <footer>{item.region} · qty {item.quantity}</footer>
                </a>
              </li>
            ))}
          </ul>
      }
    </main>
  );
}
