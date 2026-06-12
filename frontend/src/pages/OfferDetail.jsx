import { useEffect, useState } from 'react';
import { offers as offersApi } from '../api/client';
import styles from './RequestDetail.module.css';

export default function OfferDetail({ id }) {
  const [offer, setOffer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    offersApi.get(id)
      .then(setOffer)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p className={styles.status}>Loading…</p>;
  if (error)   return <p className={styles.status}>Could not load offer: {error}</p>;

  return (
    <main className={styles.page}>
      <a href="/offers" className={styles.back} style={{ color: '#2e7d32' }}>← Back to Offers</a>
      <div className={styles.card}>
        <span className={styles.category} style={{ color: '#2e7d32' }}>{offer.category}</span>
        <h1>{offer.title}</h1>
        <p className={styles.description}>{offer.description}</p>
        <dl className={styles.meta}>
          <div>
            <dt>Region</dt>
            <dd>{offer.region}</dd>
          </div>
          <div>
            <dt>Quantity available</dt>
            <dd>{offer.quantity}</dd>
          </div>
          <div>
            <dt>Posted</dt>
            <dd>{new Date(offer.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}</dd>
          </div>
        </dl>
      </div>
    </main>
  );
}
