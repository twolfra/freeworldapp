import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { likes as likesApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t, tCat, tp } from '../i18n';
import { Card, Skeleton } from '../components/ui';
import styles from './Subscriptions.module.css';

const PAGE_SIZE = 12;

export default function Likes() {
  const { user: currentUser } = useAuth();
  const [likedPosts, setLikedPosts] = useState([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!currentUser) { setLoading(false); return; }
    likesApi.getUserLikes(currentUser.id)
      .then(setLikedPosts)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (!currentUser) return (
    <main className={styles.page}>
      <p className={styles.status}>
        Please <Link to="/login">{t('subs.signInLink')}</Link> {t('likes.signIn').replace('Please {link} ', '')}
      </p>
    </main>
  );

  if (loading) return (
    <main className={styles.page} aria-busy="true">
      <div className={styles.header}>
        <h2>{t('likes.title')}</h2>
      </div>
      <ul className={styles.feed}>
        {Array.from({ length: 6 }, (_, i) => (
          <li key={i}>
            <Card><Skeleton variant="text" lines={3} /></Card>
          </li>
        ))}
      </ul>
    </main>
  );
  if (error)   return <p className={styles.status}>Could not load likes: {error}</p>;

  const totalPages = Math.max(1, Math.ceil(likedPosts.length / PAGE_SIZE));
  const safePage   = Math.min(page, totalPages);
  const pageItems  = likedPosts.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  return (
    <main className={styles.page}>
      <div className={styles.header}>
        <h2>{t('likes.title')}</h2>
      </div>
      {likedPosts.length === 0
        ? <p className={styles.empty}>{t('likes.empty')}</p>
        : <>
            <ul className={styles.feed}>
              {pageItems.map((item) => (
                <li key={`${item.type}-${item.id}`}>
                  <Link to={`/${item.type}s/${item.id}`} className={styles.card}>
                    <div className={styles.cardTop}>
                      <span className={`${styles.badge} ${item.type === 'offer' ? styles.badgeOffer : styles.badgeRequest}`}>
                        {item.type === 'offer' ? t('subs.offer') : t('subs.request')}
                      </span>
                      <span className={styles.category}>{tCat(item.category)}</span>
                      <span className={styles.meta}>
                        by <Link to={`/users/${item.authorId}`} className={styles.authorLink} onClick={(e) => e.stopPropagation()}>{item.authorUsername}</Link>
                        {' · '}
                        {new Date(item.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                      </span>
                    </div>
                    <h3>{item.title}</h3>
                    <p>{item.description}</p>
                    <footer>{item.region} · {t('list.qty')} {item.quantity}</footer>
                  </Link>
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
