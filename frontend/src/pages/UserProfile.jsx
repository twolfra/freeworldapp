import { useEffect, useState } from 'react';
import { users, offers as offersApi, requests as requestsApi } from '../api/client';
import styles from './UserProfile.module.css';

export default function UserProfile({ id }) {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  const [user, setUser]       = useState(null);
  const [offers, setOffers]   = useState([]);
  const [reqs, setReqs]       = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  useEffect(() => {
    Promise.all([
      users.get(id),
      offersApi.listByUser(id),
      requestsApi.listByUser(id),
    ])
      .then(([u, o, r]) => { setUser(u); setOffers(o); setReqs(r); })
      .catch(() => setError('User not found.'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p className={styles.status}>Loading…</p>;
  if (error)   return <p className={styles.status}>{error}</p>;

  const isSelf = currentUser?.id === id;
  const memberSince = new Date(user.createdAt).toLocaleDateString(undefined, {
    year: 'numeric', month: 'long',
  });

  return (
    <main className={styles.page}>
      <div className={styles.hero}>
        <div className={styles.avatar}>{user.username[0].toUpperCase()}</div>
        <div className={styles.heroInfo}>
          <h1 className={styles.username}>@{user.username}</h1>
          <p className={styles.since}>Member since {memberSince}</p>
          <div className={styles.statsRow}>
            <span className={styles.stat}>{offers.length} offer{offers.length !== 1 ? 's' : ''}</span>
            <span className={styles.stat}>{reqs.length} request{reqs.length !== 1 ? 's' : ''}</span>
            {!isSelf && currentUser && (
              <a href={`/messages/${id}`} className={styles.msgBtn}>
                Contact
              </a>
            )}
            {!isSelf && !currentUser && (
              <a href="/login" className={styles.msgBtn}>Sign in to message</a>
            )}
          </div>
        </div>
      </div>

      <section className={styles.section}>
        <h2>Offers</h2>
        {offers.length === 0
          ? <p className={styles.empty}>No offers yet.</p>
          : <ul className={styles.grid}>
              {offers.map((o) => (
                <li key={o.id}>
                  <a href={`/offers/${o.id}`} className={styles.card}>
                    <span className={styles.category} style={{ color: '#2e7d32' }}>{o.category}</span>
                    <h3>{o.title}</h3>
                    <p>{o.description}</p>
                    <footer>{o.region} · qty {o.quantity}</footer>
                  </a>
                </li>
              ))}
            </ul>
        }
      </section>

      <section className={styles.section}>
        <h2>Requests</h2>
        {reqs.length === 0
          ? <p className={styles.empty}>No requests yet.</p>
          : <ul className={styles.grid}>
              {reqs.map((r) => (
                <li key={r.id}>
                  <a href={`/requests/${r.id}`} className={styles.card}>
                    <span className={styles.category}>{r.category}</span>
                    <h3>{r.title}</h3>
                    <p>{r.description}</p>
                    <footer>{r.region} · qty {r.quantity}</footer>
                  </a>
                </li>
              ))}
            </ul>
        }
      </section>
    </main>
  );
}
