import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { users, offers as offersApi, requests as requestsApi, subscriptions as subsApi, notifications } from '../api/client';
import { t, tCat } from '../i18n';
import ReportButton from '../components/ReportButton';
import styles from './UserProfile.module.css';

export default function UserProfile() {
  const { id } = useParams();
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  const [user, setUser]             = useState(null);
  const [offers, setOffers]         = useState([]);
  const [reqs, setReqs]             = useState([]);
  const [subscribed, setSubscribed] = useState(false);
  const [subLoading, setSubLoading] = useState(false);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [notifyOnMessage, setNotifyOnMessage] = useState(currentUser?.notifyOnMessage ?? true);
  const [notifySaving, setNotifySaving]       = useState(false);

  const isSelf = currentUser?.id === id;

  const handleToggleNotify = async () => {
    const next = !notifyOnMessage;
    setNotifySaving(true);
    try {
      await notifications.updatePreferences({ notifyOnMessage: next });
      setNotifyOnMessage(next);
      // Keep localStorage in sync so the toggle survives reloads.
      const stored = JSON.parse(localStorage.getItem('currentUser') || 'null');
      if (stored) {
        stored.notifyOnMessage = next;
        localStorage.setItem('currentUser', JSON.stringify(stored));
      }
    } finally {
      setNotifySaving(false);
    }
  };

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

  return (
    <main className={styles.page}>
      <div className={styles.hero}>
        <div className={styles.avatar}>{user.username[0].toUpperCase()}</div>
        <div className={styles.heroInfo}>
          <h1 className={styles.username}>{user.username}</h1>
          <p className={styles.since}>{t('profile.memberSince')} {memberSince}</p>
          <div className={styles.statsRow}>
            <span className={styles.stat}>{offers.length} {offers.length !== 1 ? t('profile.offers') : t('profile.offer')}</span>
            <span className={styles.stat}>{reqs.length} {reqs.length !== 1 ? t('profile.requests') : t('profile.request')}</span>
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
          </div>
        </div>
      </div>

      {isSelf && (
        <section className={styles.section}>
          <h2>{t('profile.settings')}</h2>
          <label className={styles.notifyRow}>
            <input
              type="checkbox"
              checked={notifyOnMessage}
              onChange={handleToggleNotify}
              disabled={notifySaving}
            />
            <span>{t('profile.notifyMessages')}</span>
          </label>
          <p className={styles.notifyHint}>{t('profile.notifyMessagesHint')}</p>
        </section>
      )}

      <section className={styles.section}>
        <h2>{t('profile.offersSection')}</h2>
        {offers.length === 0
          ? <p className={styles.empty}>{t('profile.noOffers')}</p>
          : <ul className={styles.grid}>
              {offers.map((o) => (
                <li key={o.id}>
                  <Link to={`/offers/${o.id}`} className={styles.card}>
                    <span className={styles.category} style={{ color: '#2e7d32' }}>{tCat(o.category)}</span>
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
    </main>
  );
}
