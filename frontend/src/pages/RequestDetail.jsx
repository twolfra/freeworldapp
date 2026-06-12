import { useEffect, useState } from 'react';
import { requests as requestsApi } from '../api/client';
import styles from './RequestDetail.module.css';

export default function RequestDetail({ id }) {
  const [request, setRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');

  useEffect(() => {
    requestsApi.get(id)
      .then(setRequest)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p className={styles.status}>Loading…</p>;
  if (error)   return <p className={styles.status}>Could not load request: {error}</p>;

  const isOwnPost = currentUser?.id === request.requestedById;

  return (
    <main className={styles.page}>
      <a href="/requests" className={styles.back}>← Back to Requests</a>
      <div className={styles.card}>
        <span className={styles.category}>{request.category}</span>
        <h1>{request.title}</h1>
        <div className={styles.authorRow}>
          <span>Posted by</span>
          <a href={`/users/${request.requestedById}`} className={styles.authorLink}>
            @{request.requestedByUsername}
          </a>
          {!isOwnPost && currentUser && (
            <a href={`/messages/${request.requestedById}`} className={styles.contactBtn}>
              Contact
            </a>
          )}
          {!currentUser && (
            <a href="/login" className={styles.contactBtn}>Sign in to contact</a>
          )}
        </div>
        <p className={styles.description}>{request.description}</p>
        <dl className={styles.meta}>
          <div>
            <dt>Region</dt>
            <dd>{request.region}</dd>
          </div>
          <div>
            <dt>Quantity needed</dt>
            <dd>{request.quantity}</dd>
          </div>
          <div>
            <dt>Posted</dt>
            <dd>{new Date(request.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}</dd>
          </div>
        </dl>
      </div>
    </main>
  );
}
