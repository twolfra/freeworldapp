import { useEffect, useState } from 'react';
import { auth, messages as messagesApi } from '../api/client';
import styles from './Navbar.module.css';

export default function Navbar() {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (!currentUser?.token) return;

    const refreshCount = () => {
      messagesApi.getUnreadCount(currentUser.id)
        .then((r) => setUnread(r.count))
        .catch(() => {});
    };

    refreshCount();

    const es = new EventSource(
      `/api/messages/stream?userId=${currentUser.id}&token=${currentUser.token}`
    );
    es.addEventListener('message', refreshCount);
    es.addEventListener('read', refreshCount);

    return () => es.close();
  }, [currentUser?.id]);

  async function signOut() {
    await auth.logout().catch(() => {});
    localStorage.removeItem('currentUser');
    window.location.href = '/';
  }

  return (
    <nav className={styles.nav}>
      <a href="/" className={styles.brand}>🌍 FreeWorld</a>
      <div className={styles.links}>
        <a href="/offers">Offers</a>
        <a href="/requests">Requests</a>
        <a href="/offers/new">Make an Offer</a>
        <a href="/requests/new">Make a Request</a>
        {currentUser && (
          <a href="/messages" className={styles.messagesLink}>
            Messages
            {unread > 0 && <span className={styles.badge}>{unread > 99 ? '99+' : unread}</span>}
          </a>
        )}
        {currentUser && <a href="/subscriptions">Subscriptions</a>}
        {!currentUser && <a href="/login">Sign In</a>}
        {!currentUser && <a href="/register">Join</a>}
        {currentUser && (
          <a href={`/users/${currentUser.id}`} className={styles.userChip}>@{currentUser.username}</a>
        )}
        {currentUser && (
          <button className={styles.signOutBtn} onClick={signOut}>Sign out</button>
        )}
      </div>
    </nav>
  );
}
