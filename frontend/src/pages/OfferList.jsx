import { useEffect, useState } from 'react';
import { offers as offersApi } from '../api/client';
import styles from './OfferList.module.css';

export default function OfferList() {
  const [offers, setOffers]   = useState([]);
  const [query, setQuery]     = useState('');
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

  const q = query.trim().toLowerCase();
  const filtered = q
    ? offers.filter((o) =>
        o.title.toLowerCase().includes(q) ||
        o.description.toLowerCase().includes(q) ||
        o.region.toLowerCase().includes(q) ||
        o.category.toLowerCase().includes(q)
      )
    : offers;

  return (
    <main className={styles.page}>
      <div className={styles.header}>
        <h2>Community Offers</h2>
        <a href="/offers/new" className="btn-primary" style={{ borderRadius: 6, background: '#2e7d32', color: '#fff', padding: '0.5rem 1.2rem', fontFamily: 'inherit' }}>+ Make an Offer</a>
      </div>
      <div className={styles.searchBar}>
        <input
          className={styles.searchInput}
          type="search"
          placeholder="Search offers by title, region, category…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          autoComplete="off"
        />
      </div>
      {filtered.length === 0
        ? <p className={styles.empty}>{q ? `No offers matching "${query}".` : 'No offers yet — be the first to give something!'}</p>
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
