import { useEffect, useState } from 'react';
import { requests as requestsApi } from '../api/client';
import styles from './OfferList.module.css';

export default function RequestList() {
  const [requests, setRequests] = useState([]);
  const [region, setRegion]     = useState('');
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState(null);

  useEffect(() => {
    requestsApi.list()
      .then(setRequests)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className={styles.status}>Loading requests…</p>;
  if (error)   return <p className={styles.status}>Could not load requests: {error}</p>;

  const regions  = [...new Set(requests.map((r) => r.region))].sort();
  const filtered = region ? requests.filter((r) => r.region === region) : requests;

  return (
    <main className={styles.page}>
      <div className={styles.header}>
        <h2>Community Requests</h2>
        <a href="/requests/new" className="btn-primary" style={{ borderRadius: 6, background: '#1565c0', color: '#fff', padding: '0.5rem 1.2rem', fontFamily: 'inherit' }}>+ Make a Request</a>
      </div>
      <div className={styles.filterBar}>
        <label htmlFor="req-region">Region</label>
        <select
          id="req-region"
          className={styles.filterSelect}
          value={region}
          onChange={(e) => setRegion(e.target.value)}
        >
          <option value="">All regions</option>
          {regions.map((r) => <option key={r} value={r}>{r}</option>)}
        </select>
        {region && (
          <button className={styles.clearBtn} onClick={() => setRegion('')}>✕ Clear</button>
        )}
      </div>
      {filtered.length === 0
        ? <p className={styles.empty}>{region ? `No requests in "${region}".` : 'No requests yet — be the first to ask for something!'}</p>
        : <ul className={styles.grid}>
            {filtered.map((r) => (
              <li key={r.id}>
                <a href={`/requests/${r.id}`} className={styles.card} style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', textDecoration: 'none', cursor: 'pointer' }}>
                  <span className={styles.category} style={{ color: '#1565c0' }}>{r.category}</span>
                  <h3>{r.title}</h3>
                  <p>{r.description}</p>
                  <footer>{r.region} · qty {r.quantity}</footer>
                </a>
              </li>
            ))}
          </ul>
      }
    </main>
  );
}
