import { useEffect, useState } from 'react';
import { offers as offersApi } from '../api/client';
import styles from './OfferList.module.css';

export default function OfferList() {
  const [offers, setOffers]   = useState([]);
  const [region, setRegion]   = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  useEffect(() => {
    offersApi.list()
      .then(setOffers)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className={styles.status}>Loading offers…</p>;
  if (error)   return <p className={styles.status}>Could not load offers: {error}</p>;

  const regions  = [...new Set(offers.map((o) => o.region))].sort();
  const filtered = region ? offers.filter((o) => o.region === region) : offers;

  return (
    <main className={styles.page}>
      <div className={styles.header}>
        <h2>Community Offers</h2>
        <a href="/offers/new" className="btn-primary" style={{ borderRadius: 6, background: '#2e7d32', color: '#fff', padding: '0.5rem 1.2rem', fontFamily: 'inherit' }}>+ Make an Offer</a>
      </div>
      <div className={styles.filterBar}>
        <label htmlFor="offer-region">Region</label>
        <select
          id="offer-region"
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
        ? <p className={styles.empty}>{region ? `No offers in "${region}".` : 'No offers yet — be the first to give something!'}</p>
        : <ul className={styles.grid}>
            {filtered.map((o) => (
              <li key={o.id}>
                <a href={`/offers/${o.id}`} className={styles.card} style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', textDecoration: 'none', cursor: 'pointer' }}>
                  <span className={styles.category}>{o.category}</span>
                  <h3>{o.title}</h3>
                  <p>{o.description}</p>
                  <footer>{o.region} · qty {o.quantity}</footer>
                </a>
              </li>
            ))}
          </ul>
      }
    </main>
  );
}
