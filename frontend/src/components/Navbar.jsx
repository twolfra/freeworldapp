import styles from './Navbar.module.css';

export default function Navbar() {
  return (
    <nav className={styles.nav}>
      <a href="/" className={styles.brand}>🌍 FreeWorld</a>
      <div className={styles.links}>
        <a href="/offers">Offers</a>
        <a href="/requests">Requests</a>
        <a href="/offers/new">Make an Offer</a>
        <a href="/requests/new">Make a Request</a>
        <a href="/login">Sign In</a>
        <a href="/register">Join</a>
      </div>
    </nav>
  );
}
