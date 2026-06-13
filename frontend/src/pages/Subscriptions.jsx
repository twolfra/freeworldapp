import { useEffect, useState } from 'react';
import { subscriptions as subsApi } from '../api/client';
import { t, tCat, tp } from '../i18n';
import styles from './Subscriptions.module.css';

const PAGE_SIZE = 12;

export default function Subscriptions() {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  const [feed, setFeed]       = useState([]);
  const [page, setPage]       = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  useEffect(() => {
    if (!currentUser) { setLoading(false); return; }
    subsApi.feed(currentUser.id)
      .then(setFeed)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (!currentUser) return (
    <main className={styles.page}>
      <p className={styles.status}>
        Please <a href="/login">{t('subs.signInLink')}</a> {t('subs.signIn').replace('Please {link} ', '')}
      </p>
    </main>
  );

  if (loading) return <p className={styles.status}>{t('detail.loading')}</p>;
  if (error)   return <p className={styles.status}>Could not load feed: {error}</p>;

  const totalPages = Math.max(1, Math.ceil(feed.length / PAGE_SIZE));
  const safePage   = Math.min(page, totalPages);
  const pageItems  = feed.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  return (
    <main className={styles.page}>
      <div className={styles.header}>
        <h2>{t('subs.title')}</h2>
      </div>
      {feed.length === 0
        ? <p className={styles.empty}>
            {t('subs.emptyStart')}{' '}
            <a href="/offers">{t('subs.browseOffers')}</a>{' '}
            {t('subs.browseOr')}{' '}
            <a href="/requests">{t('subs.browseRequests')}</a>{' '}
            {t('subs.emptyEnd')}
          </p>
        : <>
            <ul className={styles.feed}>
              {pageItems.map((item) => (
                <li key={`${item.type}-${item.id}`}>
                  <a href={`/${item.type}s/${item.id}`} className={styles.card}>
                    <div className={styles.cardTop}>
                      <span className={`${styles.badge} ${item.type === 'offer' ? styles.badgeOffer : styles.badgeRequest}`}>
                        {item.type === 'offer' ? t('subs.offer') : t('subs.request')}
                      </span>
                      <span className={styles.category}>{tCat(item.category)}</span>
                      <span className={styles.meta}>
                        by <a href={`/users/${item.authorId}`} className={styles.authorLink} onClick={(e) => e.stopPropagation()}>@{item.authorUsername}</a>
                        {' · '}
                        {new Date(item.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                      </span>
                    </div>
                    <h3>{item.title}</h3>
                    <p>{item.description}</p>
                    <footer>{item.region} · {t('list.qty')} {item.quantity}</footer>
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
