import { useEffect, useState } from 'react';
import { requests as requestsApi } from '../api/client';
import styles from './OfferList.module.css';

const PAGE_SIZE = 12;

export default function RequestList() {
  const [requests, setRequests] = useState([]);
  const [query, setQuery]       = useState('');
  const [region, setRegion]     = useState('');
  const [page, setPage]         = useState(1);
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

  const regions = [...new Set(requests.map((r) => r.region))].sort();
  const q = query.trim().toLowerCase();
  const filtered = requests.filter((r) =>
    (!region || r.region === region) &&
    (!q ||
      r.title.toLowerCase().includes(q) ||
      r.description.toLowerCase().includes(q) ||
      r.region.toLowerCase().includes(q) ||
      r.category.toLowerCase().includes(q))
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
        <h2>Community Requests</h2>
        <a href="/requests/new" className="btn-primary" style={{ borderRadius: 6, background: '#1565c0', color: '#fff', padding: '0.5rem 1.2rem', fontFamily: 'inherit' }}>+ Make a Request</a>
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
        ? <p className={styles.empty}>No requests match your search.</p>
        : <>
            <ul className={styles.grid}>
              {pageItems.map((r) => (
                <li key={r.id}>
                  <a href={`/requests/${r.id}`} className={styles.card}>
                    {r.imageUrl && <img src={r.imageUrl} className={styles.cardImage} alt={r.title} />}
                    <span className={styles.category} style={{ color: '#1565c0' }}>{r.category}</span>
                    <h3>{r.title}</h3>
                    <p>{r.description}</p>
                    <footer>{r.region} · qty {r.quantity}</footer>
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
