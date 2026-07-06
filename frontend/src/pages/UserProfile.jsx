import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { users, offers as offersApi, requests as requestsApi, subscriptions as subsApi, thanks as thanksApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t, tCat, tp } from '../i18n';
import ReportButton from '../components/ReportButton';
import { Avatar, Button } from '../components/ui';
import styles from './UserProfile.module.css';

export default function UserProfile() {
  const { id } = useParams();
  const { user: currentUser } = useAuth();
  const [user, setUser]             = useState(null);
  const [offers, setOffers]         = useState([]);
  const [reqs, setReqs]             = useState([]);
  const [subscribed, setSubscribed] = useState(false);
  const [subLoading, setSubLoading] = useState(false);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [thanksList, setThanksList]           = useState([]);

  const isSelf = currentUser?.id === id;

  useEffect(() => {
    const fetches = [
      users.get(id),
      offersApi.listByUser(id),
      requestsApi.listByUser(id),
    ];
    if (currentUser && !isSelf) {
      fetches.push(subsApi.check(currentUser.id, id));
    }
    Promise.all(fetches)
      .then(([u, o, r, checkRes]) => {
        setUser(u); setOffers(o); setReqs(r);
        document.title = `${u.username} — FreeWorld`;
        if (checkRes) setSubscribed(checkRes.subscribed);
      })
      .catch(() => setError(t('profile.error')))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    thanksApi.forUser(id)
      .then(setThanksList)
      .catch(() => setThanksList([]));
  }, [id]);

  const handleSubscribe = async () => {
    if (!currentUser) return;
    setSubLoading(true);
    try {
      if (subscribed) {
        await subsApi.unsubscribe(currentUser.id, id);
        setSubscribed(false);
      } else {
        await subsApi.subscribe({ subscriberId: currentUser.id, subscribedToId: id });
        setSubscribed(true);
      }
    } finally {
      setSubLoading(false);
    }
  };

  if (loading) return <p className={styles.status}>{t('detail.loading')}</p>;
  if (error)   return <p className={styles.status}>{error}</p>;

  const memberSince = new Date(user.createdAt).toLocaleDateString(undefined, {
    year: 'numeric', month: 'long',
  });

  const givenCount = offers.filter((o) => o.status === 'GIVEN').length;

  return (
    <main className={styles.page}>
      <div className={styles.hero}>
        <Avatar
          src={user.avatarUrl}
          name={user.displayName || user.username}
          size="lg"
          className={styles.heroAvatar}
        />
        <div className={styles.heroInfo}>
          <h1 className={styles.username}>{user.displayName || user.username}</h1>
          {user.displayName && <p className={styles.handle}>@{user.username}</p>}
          <p className={styles.since}>
            {t('profile.memberSince')} {memberSince}
            {user.city && <span className={styles.city}> · 📍 {user.city}</span>}
          </p>
          {user.bio && <p className={styles.bio}>{user.bio}</p>}
          <div className={styles.statsRow}>
            <span className={styles.stat}>{offers.length} {offers.length !== 1 ? t('profile.offers') : t('profile.offer')}</span>
            <span className={styles.stat}>{reqs.length} {reqs.length !== 1 ? t('profile.requests') : t('profile.request')}</span>
            {givenCount > 0 && (
              <span className={styles.stat}>
                {givenCount === 1 ? t('profile.givenAwayOne') : tp('profile.givenAway', { n: givenCount })}
              </span>
            )}
            {!isSelf && currentUser && (
              <>
                <Link to={`/messages/${id}`} className={styles.msgBtn}>{t('profile.contact')}</Link>
                <button
                  className={subscribed ? styles.subBtnActive : styles.subBtn}
                  onClick={handleSubscribe}
                  disabled={subLoading}
                >
                  {subscribed ? t('profile.subscribed') : t('profile.subscribe')}
                </button>
                <ReportButton targetType="USER" targetId={id} />
              </>
            )}
            {!isSelf && !currentUser && (
              <Link to="/login" className={styles.msgBtn}>{t('profile.signInContact')}</Link>
            )}
            {isSelf && (
              <Button as={Link} to="/settings" variant="secondary" size="sm">
                ⚙️ {t('profile.settings')}
              </Button>
            )}
          </div>
        </div>
      </div>

      <section className={styles.section}>
        <h2>{t('profile.offersSection')}</h2>
        {offers.length === 0
          ? <p className={styles.empty}>{t('profile.noOffers')}</p>
          : <ul className={styles.grid}>
              {offers.map((o) => (
                <li key={o.id}>
                  <Link to={`/offers/${o.id}`} className={styles.card}>
                    <span className={styles.category} style={{ color: 'var(--green)' }}>{tCat(o.category)}</span>
                    <h3>{o.title}</h3>
                    <p>{o.description}</p>
                    <footer>{o.region} · {t('list.qty')} {o.quantity}</footer>
                  </Link>
                </li>
              ))}
            </ul>
        }
      </section>

      <section className={styles.section}>
        <h2>{t('profile.requestsSection')}</h2>
        {reqs.length === 0
          ? <p className={styles.empty}>{t('profile.noRequests')}</p>
          : <ul className={styles.grid}>
              {reqs.map((r) => (
                <li key={r.id}>
                  <Link to={`/requests/${r.id}`} className={styles.card}>
                    <span className={styles.category}>{tCat(r.category)}</span>
                    <h3>{r.title}</h3>
                    <p>{r.description}</p>
                    <footer>{r.region} · {t('list.qty')} {r.quantity}</footer>
                  </Link>
                </li>
              ))}
            </ul>
        }
      </section>

      {thanksList.length > 0 && (
        <section className={styles.section}>
          <h2>{t('profile.thanksSection')} 🎁</h2>
          <ul className={styles.thanksList}>
            {thanksList.map((th) => (
              <li key={th.id} className={styles.thanksItem}>
                <div className={styles.thanksHead}>
                  <Link to={`/users/${th.fromUserId}`} className={styles.thanksFrom}>
                    @{th.fromUsername}
                  </Link>
                  {th.offerTitle && (
                    <span className={styles.thanksMeta}>
                      {t('profile.thanksFor')} “{th.offerTitle}”
                    </span>
                  )}
                  <span className={styles.thanksMeta}>
                    · {new Date(th.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}
                  </span>
                  {currentUser && currentUser.id !== th.fromUserId && (
                    <ReportButton targetType="THANKS" targetId={th.id} />
                  )}
                </div>
                {th.text && <p className={styles.thanksText}>“{th.text}”</p>}
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  );
}
