import styles from './Navbar.module.css';

export default function Navbar() {
  return (
    <nav className={styles.nav}>
      <a href="/" className={styles.brand}>🌍 FreeWorld</a>
      <div className={styles.links}>
        <a href="/offers">Browse Offers</a>
        <a href="/offers/new">Make an Offer</a>
        <a href="/register">Join</a>
      </div>
    </nav>
  );
}
