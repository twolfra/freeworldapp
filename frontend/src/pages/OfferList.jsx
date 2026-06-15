import { useEffect, useState } from 'react';
import { offers as offersApi } from '../api/client';
import { t, tCat, tp } from '../i18n';
import styles from './OfferList.module.css';

const CATEGORIES = [
  'Food & Drink', 'Clothing', 'Books & Media', 'Tools & Equipment',
  'Furniture', 'Electronics', 'Skills & Services', 'Plants & Seeds',
  'Childcare', 'Transport', 'Other',
];

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

  if (loading) return <p className={styles.status}>{t('home.loading')}</p>;
  if (error)   return <p className={styles.status}>{error}</p>;

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
      {/* ── Two-column layout ── */}
      <div className={styles.layout}>
        {/* Sidebar */}
        <aside className={styles.sidebar}>
          <div className={styles.sideBox}>
            <h3 className={styles.sideTitle}>{t('home.categories')}</h3>
            <ul className={styles.catList}>
              {CATEGORIES.map((c) => (
                <li key={c}>
                  <a href={`/offers?q=${encodeURIComponent(c)}`} className={styles.catRow}>
                    {tCat(c)}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </aside>

        {/* Main */}
        <div className={styles.main}>
          <div className={styles.header}>
            <h2>{t('offers.heading')}<span className={styles.count}>{tp('offers.count', { n: filtered.length })}</span></h2>
            <a href="/offers/new" className="btn-accent">{t('offers.cta')}</a>
          </div>
          <div className={styles.filterBar}>
            <select
              className={styles.filterSelect}
              value="offer"
              onChange={(e) => {
                const q = query ? `?q=${encodeURIComponent(query)}` : '';
                if (e.target.value === 'listings') window.location.href = `/${q}`;
                if (e.target.value === 'request')  window.location.href = `/requests${q}`;
              }}
            >
              <option value="listings">{t('home.typeListings')}</option>
              <option value="offer">{t('home.typeOffers')}</option>
              <option value="request">{t('home.typeRequests')}</option>
            </select>
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
            ? <p className={styles.empty}>{t('offers.noMatch')}</p>
            : <>
                <ul className={styles.grid}>
                  {pageItems.map((o) => (
                    <li key={o.id}>
                      <a href={`/offers/${o.id}`} className={styles.card}>
                        <div className={styles.thumb}>
                          {o.imageUrl
                            ? <img src={o.imageUrl} className={styles.cardImage} alt={o.title} />
                            : <div className={styles.thumbEmpty} />}
                          <span className={styles.categoryPill}>{tCat(o.category)}</span>
                        </div>
                        <div className={styles.body}>
                          <h3>{o.title}</h3>
                          <p className={styles.desc}>{o.description}</p>
                          <span className={styles.price}>{t('offers.priceTag')}</span>
                          <div className={styles.meta}>
                            <span>{o.region}</span>
                            <span className={styles.dot}>·</span>
                            <span>{t('list.qty')} {o.quantity}</span>
                          </div>
                        </div>
                      </a>
                    </li>
                  ))}
                </ul>
                {totalPages > 1 && (
                  <div className={styles.pagination}>
                    <button className={styles.pageBtn} onClick={() => setPage((p) => p - 1)} disabled={safePage === 1}>{t('list.pagePrev')}</button>
                    <span className={styles.pageInfo}>{tp('list.pageInfo', { n: safePage, total: totalPages })}</span>
                    <button className={styles.pageBtn} onClick={() => setPage((p) => p + 1)} disabled={safePage === totalPages}>{t('list.pageNext')}</button>
                  </div>
                )}
              </>
          }
        </div>
      </div>
    </main>
  );
}
