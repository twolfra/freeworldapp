import styles from './Navbar.module.css';

export default function Navbar() {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');

  return (
    <nav className={styles.nav}>
      <a href="/" className={styles.brand}>🌍 FreeWorld</a>
      <div className={styles.links}>
        <a href="/offers">Offers</a>
        <a href="/requests">Requests</a>
        <a href="/offers/new">Make an Offer</a>
        <a href="/requests/new">Make a Request</a>
        {currentUser && <a href="/messages">Messages</a>}
        {currentUser && <a href="/subscriptions">Subscriptions</a>}
        {!currentUser && <a href="/login">Sign In</a>}
        {!currentUser && <a href="/register">Join</a>}
        {currentUser && <a href="/login" className={styles.userChip}>@{currentUser.username}</a>}
      </div>
    </nav>
  );
}
