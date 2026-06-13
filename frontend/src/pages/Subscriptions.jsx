import { useEffect, useState } from 'react';
import { subscriptions as subsApi } from '../api/client';
import styles from './Subscriptions.module.css';

const PAGE_SIZE = 12;

export default function Subscriptions() {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  const [feed, setFeed]       = useState([]);
  const [page, setPage]       = useState(1);
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

  const totalPages = Math.max(1, Math.ceil(feed.length / PAGE_SIZE));
  const safePage   = Math.min(page, totalPages);
  const pageItems  = feed.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  return (
    <main className={styles.page}>
      <div className={styles.header}>
        <h2>Subscriptions</h2>
      </div>
      {feed.length === 0
        ? <p className={styles.empty}>Nothing here yet. <a href="/offers">Browse offers</a> or <a href="/requests">requests</a> and subscribe to users you want to follow.</p>
        : <>
            <ul className={styles.feed}>
              {pageItems.map((item) => (
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
            {totalPages > 1 && (
              <div className={styles.pagination}>
                <button
                  className={styles.pageBtn}
                  onClick={() => setPage((p) => p - 1)}
                  disabled={safePage === 1}
                >← Prev</button>
                <span className={styles.pageInfo}>Page {safePage} of {totalPages}</span>
                <button
                  className={styles.pageBtn}
                  onClick={() => setPage((p) => p + 1)}
                  disabled={safePage === totalPages}
                >Next →</button>
              </div>
            )}
          </>
      }
    </main>
  );
}
