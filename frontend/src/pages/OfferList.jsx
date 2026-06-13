import { useEffect, useState } from 'react';
import { offers as offersApi } from '../api/client';
import styles from './OfferList.module.css';

const PAGE_SIZE = 12;

export default function OfferList() {
  const initialQuery = new URLSearchParams(window.location.search).get('q') || '';
  const [offers, setOffers]   = useState([]);
  const [query, setQuery]     = useState(initialQuery);
  const [region, setRegion]   = useState('');
  const [page, setPage]       = useState(1);
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

  const regions = [...new Set(offers.map((o) => o.region))].sort();
  const q = query.trim().toLowerCase();
  const filtered = offers.filter((o) =>
    (!region || o.region === region) &&
    (!q ||
      o.title.toLowerCase().includes(q) ||
      o.description.toLowerCase().includes(q) ||
      o.region.toLowerCase().includes(q) ||
      o.category.toLowerCase().includes(q))
  );

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const pageItems = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  function handleFilterChange(setter) {
    return (e) => { setter(e.target.value); setPage(1); };
  }

  return (
    <main className={styles.page}>
      <div className={styles.header}>
        <h2>Offers<span className={styles.count}>{filtered.length} available</span></h2>
        <a href="/offers/new" className="btn-accent">+ Give something away</a>
      </div>
      <div className={styles.filterBar}>
        <input
          className={styles.searchInput}
          type="search"
          placeholder="Search by title, category…"
          value={query}
          onChange={handleFilterChange(setQuery)}
          autoComplete="off"
        />
        <select
          className={styles.filterSelect}
          value={region}
          onChange={handleFilterChange(setRegion)}
        >
          <option value="">All regions</option>
          {regions.map((r) => <option key={r} value={r}>{r}</option>)}
        </select>
      </div>
      {filtered.length === 0
        ? <p className={styles.empty}>No offers match your search.</p>
        : <>
            <ul className={styles.grid}>
              {pageItems.map((o) => (
                <li key={o.id}>
                  <a href={`/offers/${o.id}`} className={styles.card}>
                    <div className={styles.thumb}>
                      {o.imageUrl
                        ? <img src={o.imageUrl} className={styles.cardImage} alt={o.title} />
                        : <div className={styles.thumbEmpty}>🎁</div>}
                      <span className={styles.categoryPill}>{o.category}</span>
                    </div>
                    <div className={styles.body}>
                      <h3>{o.title}</h3>
                      <p className={styles.desc}>{o.description}</p>
                      <span className={styles.price}>Free</span>
                      <div className={styles.meta}>
                        <span>📍 {o.region}</span>
                        <span className={styles.dot}>·</span>
                        <span>qty {o.quantity}</span>
                      </div>
                    </div>
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
