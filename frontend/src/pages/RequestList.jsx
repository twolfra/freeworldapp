import { useEffect, useState } from 'react';
import { requests as requestsApi } from '../api/client';
import { t, tCat, tp } from '../i18n';
import styles from './OfferList.module.css';

const PAGE_SIZE = 12;

export default function RequestList() {
  const initialQuery = new URLSearchParams(window.location.search).get('q') || '';
  const [requests, setRequests] = useState([]);
  const [query, setQuery]       = useState(initialQuery);
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

  if (loading) return <p className={styles.status}>{t('home.loading')}</p>;
  if (error)   return <p className={styles.status}>{error}</p>;

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
        <h2>{t('requests.heading')}<span className={styles.count}>{tp('requests.count', { n: filtered.length })}</span></h2>
        <a href="/requests/new" className="btn-accent">{t('requests.cta')}</a>
      </div>
      <div className={styles.filterBar}>
        <input
          className={styles.searchInput}
          type="search"
          placeholder={t('list.searchPlaceholder')}
          value={query}
          onChange={handleFilterChange(setQuery)}
          autoComplete="off"
        />
        <select
          className={styles.filterSelect}
          value={region}
          onChange={handleFilterChange(setRegion)}
        >
          <option value="">{t('list.allRegions')}</option>
          {regions.map((r) => <option key={r} value={r}>{r}</option>)}
        </select>
      </div>
      {filtered.length === 0
        ? <p className={styles.empty}>{t('requests.noMatch')}</p>
        : <>
            <ul className={styles.grid}>
              {pageItems.map((r) => (
                <li key={r.id}>
                  <a href={`/requests/${r.id}`} className={styles.card}>
                    <div className={styles.thumb} style={{ background: 'var(--blue-light)' }}>
                      {r.imageUrl
                        ? <img src={r.imageUrl} className={styles.cardImage} alt={r.title} />
                        : <div className={styles.thumbEmpty} />}
                      <span className={styles.categoryPill} style={{ color: 'var(--blue)' }}>{tCat(r.category)}</span>
                    </div>
                    <div className={styles.body}>
                      <h3>{r.title}</h3>
                      <p className={styles.desc}>{r.description}</p>
                      <span className={styles.price} style={{ color: 'var(--blue)' }}>{t('requests.priceTag')}</span>
                      <div className={styles.meta}>
                        <span>{r.region}</span>
                        <span className={styles.dot}>·</span>
                        <span>{t('list.qty')} {r.quantity}</span>
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
                >{t('list.pagePrev')}</button>
                <span className={styles.pageInfo}>{tp('list.pageInfo', { n: safePage, total: totalPages })}</span>
                <button
                  className={styles.pageBtn}
                  onClick={() => setPage((p) => p + 1)}
                  disabled={safePage === totalPages}
                >{t('list.pageNext')}</button>
              </div>
            )}
          </>
      }
    </main>
  );
}
