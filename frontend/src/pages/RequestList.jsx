import { lazy, Suspense, useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { search as searchApi } from '../api/client';
import { t, tCat, tp } from '../i18n';
import { Badge, Button, Card, EmptyState, Skeleton } from '../components/ui';
import PostalCodeInput from '../components/PostalCodeInput';

// Lazy so Leaflet (~150 kB) only loads when the map view is opened.
const PostsMap = lazy(() => import('../components/PostsMap'));
import {
  readStoredLocation, saveStoredLocation, clearStoredLocation, formatLocation,
} from '../hooks/locationStorage';
import styles from './OfferList.module.css';

const CATEGORIES = [
  'Food & Drink', 'Clothing', 'Books & Media', 'Tools & Equipment',
  'Furniture', 'Electronics', 'Skills & Services', 'Plants & Seeds',
  'Childcare', 'Transport', 'Other',
];

const RADIUS_OPTIONS = [2, 5, 10, 25, 50];
const PAGE_SIZE = 12;
const MAP_SIZE = 200; // map view fetches more items to show more pins

export default function RequestList() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialQuery = searchParams.get('q') || '';
  const category = searchParams.get('category') || '';

  const [items, setItems]     = useState([]);
  const [total, setTotal]     = useState(0);
  const [query, setQuery]     = useState(initialQuery);
  const [debouncedQuery, setDebouncedQuery] = useState(initialQuery);
  const [page, setPage]       = useState(1);
  const [includeCompleted, setIncludeCompleted] = useState(false);
  const [location, setLocation] = useState(readStoredLocation);
  const [locationText, setLocationText] = useState(() => formatLocation(readStoredLocation()));
  const [radiusKm, setRadiusKm] = useState(10);
  const [sort, setSort]       = useState('newest');
  const [view, setView]       = useState('list'); // 'list' | 'map'
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  // Debounce typed search before hitting the server.
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  const effectiveSort = sort === 'nearest' && location ? 'nearest' : 'newest';

  useEffect(() => {
    let cancelled = false;
    searchApi.run({
      type: 'requests',
      q: debouncedQuery.trim() || undefined,
      category: category || undefined,
      lat: location?.lat,
      lon: location?.lon,
      radiusKm: location ? radiusKm : undefined,
      sort: effectiveSort,
      includeCompleted: includeCompleted || undefined,
      page: view === 'map' ? 0 : page - 1,
      size: view === 'map' ? MAP_SIZE : PAGE_SIZE,
    })
      .then((res) => {
        if (cancelled) return;
        setItems(res.items);
        setTotal(res.total);
        setError(null);
      })
      .catch((e) => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [debouncedQuery, category, includeCompleted, page, location, radiusKm, effectiveSort, view]);

  function handleLocationText(text) {
    setLocationText(text);
    if (location) {
      setLocation(null);
      clearStoredLocation();
    }
    setPage(1);
  }

  function handleLocationSelect(item) {
    setLocation(item);
    setLocationText(formatLocation(item));
    saveStoredLocation(item);
    setPage(1);
  }

  function handleClearLocation() {
    setLocation(null);
    setLocationText('');
    clearStoredLocation();
    setPage(1);
  }

  function handleClearFilters() {
    setQuery('');
    setDebouncedQuery('');
    handleClearLocation();
    navigate('/requests');
  }

  if (loading) return (
    <main className={styles.page} aria-busy="true">
      <div className={styles.layout}>
        <aside className={styles.sidebar} />
        <div className={styles.main}>
          <ul className={styles.grid}>
            {Array.from({ length: 8 }, (_, i) => (
              <li key={i}>
                <Card padded={false} style={{ overflow: 'hidden' }}>
                  <Skeleton height="150px" style={{ borderRadius: 0 }} />
                  <div style={{ padding: '0.8rem' }}>
                    <Skeleton variant="text" lines={2} />
                  </div>
                </Card>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </main>
  );
  if (error)   return <p className={styles.status}>{error}</p>;

  const hasFilters = Boolean(debouncedQuery.trim() || category || location);
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const geoItemCount = items.filter((r) => r.lat != null && r.lon != null).length;

  return (
    <main className={styles.page}>
      {/* ── Two-column layout ── */}
      <div className={styles.layout}>
        {/* Sidebar */}
        <aside className={styles.sidebar}>
          <div className={styles.sideBox}>
            {/* h2: keeps heading levels sequential on the page (AP 4.2) */}
            <h2 className={styles.sideTitle}>{t('home.categories')}</h2>
            <ul className={styles.catList}>
              {CATEGORIES.map((c) => (
                <li key={c}>
                  <Link
                    to={category === c ? '/requests' : `/requests?category=${encodeURIComponent(c)}`}
                    className={category === c ? `${styles.catRow} ${styles.catRowActive}` : styles.catRow}
                    onClick={() => setPage(1)}
                  >
                    {tCat(c)}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </aside>

        {/* Main */}
        <div className={styles.main}>
          <div className={styles.header}>
            <h2>{t('requests.heading')}<span className={styles.count}>{tp('requests.count', { n: total })}</span></h2>
            <Link to="/requests/new" className="btn-accent">{t('requests.cta')}</Link>
          </div>
          <div className={styles.filterBar}>
            <select
              className={styles.filterSelect}
              value="request"
              aria-label={t('home.typeLabel')}
              onChange={(e) => {
                const q = query ? `?q=${encodeURIComponent(query)}` : '';
                if (e.target.value === 'listings') navigate(`/${q}`);
                if (e.target.value === 'offer')    navigate(`/offers${q}`);
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
              aria-label={t('list.searchPlaceholder')}
              value={query}
              onChange={(e) => { setQuery(e.target.value); setPage(1); }}
              autoComplete="off"
            />
            <label className={styles.completedToggle}>
              <input
                type="checkbox"
                checked={includeCompleted}
                onChange={(e) => { setIncludeCompleted(e.target.checked); setPage(1); }}
              />
              {t('list.showFulfilled')}
            </label>
          </div>
          {/* ── Geo filter bar (AP 3.1) + view toggle (AP 3.2) ── */}
          <div className={styles.geoBar}>
            <div className={styles.locationField}>
              <PostalCodeInput
                value={locationText}
                onChange={handleLocationText}
                onSelect={handleLocationSelect}
                placeholder={t('list.locationPlaceholder')}
                inputClassName={styles.locationInput}
              />
              {location && (
                <button
                  type="button"
                  className={styles.clearLocationBtn}
                  onClick={handleClearLocation}
                  aria-label={t('list.clearLocation')}
                  title={t('list.clearLocation')}
                >
                  ×
                </button>
              )}
            </div>
            <select
              className={styles.filterSelect}
              value={radiusKm}
              onChange={(e) => { setRadiusKm(Number(e.target.value)); setPage(1); }}
              disabled={!location}
              aria-label={t('list.radiusLabel')}
            >
              {RADIUS_OPTIONS.map((r) => <option key={r} value={r}>{r} km</option>)}
            </select>
            <div className={styles.pillGroup} role="group" aria-label={t('list.sortLabel')}>
              <button
                type="button"
                className={effectiveSort === 'newest' ? `${styles.pillBtn} ${styles.pillBtnActive}` : styles.pillBtn}
                onClick={() => { setSort('newest'); setPage(1); }}
              >
                {t('list.sortNewest')}
              </button>
              <button
                type="button"
                className={effectiveSort === 'nearest' ? `${styles.pillBtn} ${styles.pillBtnActive}` : styles.pillBtn}
                onClick={() => { setSort('nearest'); setPage(1); }}
                disabled={!location}
              >
                {t('list.sortNearest')}
              </button>
            </div>
            <div className={styles.pillGroup} role="group" aria-label={t('list.viewLabel')}>
              <button
                type="button"
                className={view === 'list' ? `${styles.pillBtn} ${styles.pillBtnActive}` : styles.pillBtn}
                onClick={() => setView('list')}
              >
                {t('list.viewList')}
              </button>
              <button
                type="button"
                className={view === 'map' ? `${styles.pillBtn} ${styles.pillBtnActive}` : styles.pillBtn}
                onClick={() => setView('map')}
              >
                {t('list.viewMap')}
              </button>
            </div>
          </div>
          {total === 0 && !hasFilters
            ? <EmptyState
                icon="🙏"
                title={t('requests.emptyTitle')}
                text={t('requests.emptyText')}
                action={<Button as={Link} to="/requests/new" variant="accent">{t('home.ask')}</Button>}
              />
            : total === 0
            ? <EmptyState
                icon="🔍"
                title={t('requests.noMatch')}
                action={
                  <Button variant="secondary" onClick={handleClearFilters}>
                    {t('list.clearFilters')}
                  </Button>
                }
              />
            : view === 'map'
            ? <>
                {geoItemCount === 0 && <p className={styles.mapEmptyHint}>{t('map.emptyGeo')}</p>}
                <Suspense fallback={<Skeleton height="60vh" />}>
                  <PostsMap items={items} basePath="/requests" center={location} />
                </Suspense>
              </>
            : <>
                <ul className={styles.grid}>
                  {items.map((r) => (
                    <li key={r.id}>
                      <Link
                        to={`/requests/${r.id}`}
                        className={r.status === 'FULFILLED' ? `${styles.card} ${styles.cardCompleted}` : styles.card}
                      >
                        <div className={styles.thumb} style={{ background: 'var(--blue-light)' }}>
                          {r.imageUrl
                            ? <img src={r.imageUrl} className={styles.cardImage} alt={r.title} />
                            : <div className={styles.thumbEmpty} />}
                          <span className={styles.categoryPill} style={{ color: 'var(--blue)' }}>{tCat(r.category)}</span>
                          {r.status === 'FULFILLED' && (
                            <Badge variant="neutral" className={styles.statusPill}>{t('status.FULFILLED')}</Badge>
                          )}
                        </div>
                        <div className={styles.body}>
                          <h3>{r.title}</h3>
                          <p className={styles.desc}>{r.description}</p>
                          <span className={styles.price} style={{ color: 'var(--blue)' }}>{t('requests.priceTag')}</span>
                          <div className={styles.meta}>
                            <span>{r.region}</span>
                            <span className={styles.dot}>·</span>
                            <span>{t('list.qty')} {r.quantity}</span>
                            {r.distanceKm != null && (
                              <span className={styles.distanceBadge}>{r.distanceKm} km</span>
                            )}
                          </div>
                        </div>
                      </Link>
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
