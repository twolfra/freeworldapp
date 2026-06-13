import { useEffect, useState } from 'react';
import { offers as offersApi, requests as requestsApi } from '../api/client';
import { t, tCat } from '../i18n';
import styles from './Home.module.css';

const CATEGORIES = [
  'Food & Drink', 'Clothing', 'Books & Media', 'Tools & Equipment',
  'Furniture', 'Electronics', 'Skills & Services', 'Plants & Seeds',
  'Childcare', 'Transport', 'Other',
];

export default function Home() {
  const [q, setQ] = useState('');
  const [recent, setRecent] = useState([]);
  const [counts, setCounts] = useState({ offers: 0, requests: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([offersApi.list(), requestsApi.list()])
      .then(([ofs, reqs]) => {
        setCounts({ offers: ofs.length, requests: reqs.length });
        const merged = [
          ...ofs.map((o) => ({ ...o, _type: 'offer' })),
          ...reqs.map((r) => ({ ...r, _type: 'request' })),
        ]
          .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
          .slice(0, 9);
        setRecent(merged);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  function search(e) {
    e.preventDefault();
    window.location.href = q.trim()
      ? `/offers?q=${encodeURIComponent(q.trim())}`
      : '/offers';
  }

  return (
    <main>
      {/* ── Hero ── */}
      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <h1>{t('home.title')} <span>{t('home.titleAccent')}</span>.</h1>
          <p>{t('home.subtitle')}</p>
          <form className={styles.searchBar} onSubmit={search}>
            <input
              type="search"
              placeholder={t('home.searchPlaceholder')}
              value={q}
              onChange={(e) => setQ(e.target.value)}
              autoComplete="off"
            />
            <button type="submit" className="btn-primary">{t('home.searchBtn')}</button>
          </form>
        </div>
      </section>

      {/* ── Two-column body ── */}
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

          <div className={styles.sideBox}>
            <h3 className={styles.sideTitle}>{t('home.stats')}</h3>
            <div className={styles.stats}>
              <a href="/offers" className={styles.stat}>
                <span className={styles.statNum} style={{ color: 'var(--green)' }}>{counts.offers}</span>
                <span className={styles.statLabel}>{t('home.statOffers')}</span>
              </a>
              <a href="/requests" className={styles.stat}>
                <span className={styles.statNum} style={{ color: 'var(--blue)' }}>{counts.requests}</span>
                <span className={styles.statLabel}>{t('home.statRequests')}</span>
              </a>
            </div>
          </div>

          <div className={styles.sideCtas}>
            <a href="/offers/new" className="btn-accent" style={{ width: '100%' }}>{t('home.give')}</a>
            <a href="/requests/new" className="btn-secondary" style={{ width: '100%' }}>{t('home.ask')}</a>
          </div>

        </aside>

        {/* Main — recent listings */}
        <div className={styles.main}>
          <div className={styles.mainHeader}>
            <h2 className={styles.mainTitle}>{t('home.recent')}</h2>
            <div className={styles.mainLinks}>
              <a href="/offers" className={styles.viewAll}>{t('home.allOffers')}</a>
              <a href="/requests" className={styles.viewAllBlue}>{t('home.allRequests')}</a>
            </div>
          </div>

          {loading ? (
            <p className={styles.empty}>{t('home.loading')}</p>
          ) : recent.length === 0 ? (
            <p className={styles.empty}>{t('home.empty')}</p>
          ) : (
            <div className={styles.grid}>
              {recent.map((item) => {
                const isOffer = item._type === 'offer';
                return (
                  <a
                    key={`${item._type}-${item.id}`}
                    href={isOffer ? `/offers/${item.id}` : `/requests/${item.id}`}
                    className={styles.card}
                  >
                    <div
                      className={styles.thumb}
                      style={{ background: isOffer ? 'var(--green-light)' : 'var(--blue-light)' }}
                    >
                      {item.imageUrl
                        ? <img src={item.imageUrl} className={styles.cardImg} alt={item.title} />
                        : <div className={styles.thumbEmpty} />}
                      <span
                        className={styles.catPill}
                        style={{ color: isOffer ? 'var(--green-dark)' : 'var(--blue)' }}
                      >{tCat(item.category)}</span>
                      <span
                        className={styles.typeBadge}
                        style={{ background: isOffer ? 'var(--green)' : 'var(--blue)' }}
                      >{isOffer ? t('home.free') : t('home.wanted')}</span>
                    </div>
                    <div className={styles.body}>
                      <h3>{item.title}</h3>
                      <p className={styles.desc}>{item.description}</p>
                      <div className={styles.meta}>
                        <span>{item.region}</span>
                        <span className={styles.dot}>·</span>
                        <span>{t('list.qty')} {item.quantity}</span>
                      </div>
                    </div>
                  </a>
                );
              })}
            </div>
          )}
        </div>

      </div>
    </main>
  );
}
