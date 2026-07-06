import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { search as searchApi } from '../api/client';
import { t, tp, tCat } from '../i18n';
import { Badge, Card, Skeleton, EmptyState } from '../components/ui';
import PostalCodeInput from '../components/PostalCodeInput';
import { readStoredLocation, saveStoredLocation, clearStoredLocation } from '../hooks/locationStorage';
import styles from './OfferList.module.css';
import ownStyles from './Search.module.css';

const CATEGORIES = [
  'Food & Drink', 'Clothing', 'Books & Media', 'Tools & Equipment',
  'Furniture', 'Electronics', 'Skills & Services', 'Plants & Seeds',
  'Childcare', 'Transport', 'Other',
];
const RADII = [2, 5, 10, 25, 50];
const PAGE_SIZE = 12;

/** One page, every filter (AP 3.5): tabs, text, category, radius, sort, image. */
export default function Search() {
  const [params] = useSearchParams();
  const [type, setType] = useState('offers');
  const [query, setQuery] = useState(params.get('q') || '');
  const [debounced, setDebounced] = useState(query);
  const [category, setCategory] = useState('');
  const [location, setLocation] = useState(() => readStoredLocation());
  const [locationText, setLocationText] = useState(() => {
    const loc = readStoredLocation();
    return loc ? `${loc.plz} ${loc.city}` : '';
  });
  const [radiusKm, setRadiusKm] = useState(10);
  const [sort, setSort] = useState('newest');
  const [withImage, setWithImage] = useState(false);
  const [page, setPage] = useState(1);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const handle = setTimeout(() => setDebounced(query), 300);
    return () => clearTimeout(handle);
  }, [query]);

  useEffect(() => {
    // no synchronous setState here (lint rule); stale results stay visible
    // during a refetch, exactly like the list pages
    let cancelled = false;
    searchApi.run({
      type,
      q: debounced.trim() || undefined,
      category: category || undefined,
      lat: location?.lat,
      lon: location?.lon,
      radiusKm: location ? radiusKm : undefined,
      sort: location && sort === 'nearest' ? 'nearest' : 'newest',
      withImage: withImage || undefined,
      page: page - 1,
      size: PAGE_SIZE,
    }).then((r) => { if (!cancelled) { setResult(r); setError(null); } })
      .catch((e) => { if (!cancelled) setError(e.message); });
    return () => { cancelled = true; };
  }, [type, debounced, category, location, radiusKm, sort, withImage, page]);

  const items = result?.items ?? [];
  const totalPages = Math.max(1, Math.ceil((result?.total ?? 0) / PAGE_SIZE));
  const isOffers = type === 'offers';
  const completed = isOffers ? 'GIVEN' : 'FULFILLED';

  function switchType(next) {
    setType(next);
    setPage(1);
  }

  return (
    <main className={ownStyles.page}>
      <h1 className={ownStyles.heading}>{t('search.heading')}</h1>

      <div className={ownStyles.tabs} role="tablist">
        <button role="tab" aria-selected={isOffers}
                className={isOffers ? ownStyles.tabActive : ownStyles.tab}
                onClick={() => switchType('offers')}>
          {t('search.tabOffers')}
        </button>
        <button role="tab" aria-selected={!isOffers}
                className={!isOffers ? ownStyles.tabActive : ownStyles.tab}
                onClick={() => switchType('requests')}>
          {t('search.tabRequests')}
        </button>
      </div>

      <div className={ownStyles.filters}>
        <input
          className={ownStyles.textInput}
          value={query}
          onChange={(e) => { setQuery(e.target.value); setPage(1); }}
          placeholder={t('list.searchPlaceholder')}
          aria-label={t('list.searchPlaceholder')}
        />
        <select value={category} aria-label={t('form.category')}
                onChange={(e) => { setCategory(e.target.value); setPage(1); }}>
          <option value="">{t('form.categoryDefault')}</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{tCat(c)}</option>)}
        </select>
        <div className={ownStyles.geoRow}>
          <PostalCodeInput
            value={locationText}
            onChange={(text) => { setLocationText(text); }}
            onSelect={(item) => {
              setLocation(item);
              saveStoredLocation(item);
              setLocationText(`${item.plz} ${item.city}`);
              setPage(1);
            }}
          />
          {location && (
            <button type="button" className={ownStyles.clearLoc}
                    onClick={() => { setLocation(null); clearStoredLocation(); setLocationText(''); setPage(1); }}
                    aria-label={t('list.locationClear')}>✕</button>
          )}
          <select value={radiusKm} disabled={!location} aria-label={t('list.radius')}
                  onChange={(e) => { setRadiusKm(Number(e.target.value)); setPage(1); }}>
            {RADII.map((r) => <option key={r} value={r}>{r} km</option>)}
          </select>
          <button type="button"
                  className={sort === 'nearest' ? ownStyles.sortActive : ownStyles.sort}
                  disabled={!location}
                  onClick={() => { setSort(sort === 'nearest' ? 'newest' : 'nearest'); setPage(1); }}>
            {sort === 'nearest' ? t('list.sortNearest') : t('list.sortNewest')}
          </button>
        </div>
        <label className={ownStyles.withImage}>
          <input type="checkbox" checked={withImage}
                 onChange={(e) => { setWithImage(e.target.checked); setPage(1); }} />
          {t('search.withImage')}
        </label>
      </div>

      {result && <p className={ownStyles.count}>{tp('search.results', { n: result.total })}</p>}
      {error && <p className={ownStyles.count}>{error}</p>}

      {!result && !error && (
        <ul className={styles.grid}>
          {Array.from({ length: 8 }, (_, i) => (
            <li key={i}>
              <Card padded={false} style={{ overflow: 'hidden' }}>
                <Skeleton height="150px" style={{ borderRadius: 0 }} />
                <div style={{ padding: '0.8rem' }}><Skeleton variant="text" lines={2} /></div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      {result && items.length === 0 && (
        <EmptyState icon="🔍" title={t('search.noResults')} text={t('list.clearFilters')} />
      )}

      {items.length > 0 && (
        <>
          <ul className={styles.grid}>
            {items.map((o) => (
              <li key={o.id}>
                <Link to={`/${type}/${o.id}`}
                      className={o.status === completed ? `${styles.card} ${styles.cardCompleted}` : styles.card}>
                  <div className={styles.thumb}>
                    {o.imageUrl
                      ? <img src={o.imageUrl} className={styles.cardImage} alt={o.title} />
                      : <div className={styles.thumbEmpty} />}
                    <span className={styles.categoryPill}>{tCat(o.category)}</span>
                    {o.status === 'RESERVED' && (
                      <Badge variant="warning" className={styles.statusPill}>{t('status.RESERVED')}</Badge>
                    )}
                  </div>
                  <div className={styles.body}>
                    <h3>{o.title}</h3>
                    <p className={styles.desc}>{o.description}</p>
                    <div className={styles.meta}>
                      <span>{o.region}</span>
                      <span className={styles.dot}>·</span>
                      <span>{t('list.qty')} {o.quantity}</span>
                      {o.distanceKm != null && (
                        <span className={styles.distanceBadge}>{o.distanceKm} km</span>
                      )}
                    </div>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
          {totalPages > 1 && (
            <div className={styles.pagination}>
              <button className={styles.pageBtn} onClick={() => setPage((p) => p - 1)} disabled={page === 1}>{t('list.pagePrev')}</button>
              <span className={styles.pageInfo}>{tp('list.pageInfo', { n: page, total: totalPages })}</span>
              <button className={styles.pageBtn} onClick={() => setPage((p) => p + 1)} disabled={page >= totalPages}>{t('list.pageNext')}</button>
            </div>
          )}
        </>
      )}
    </main>
  );
}
