import styles from './Home.module.css';

export default function Home() {
  return (
    <main className={styles.hero}>
      <h1>Give freely.<br />Receive gratefully.</h1>
      <p>FreeWorld is a gift economy marketplace — no prices, no trades, no money.<br />
        Offer what you can. Request what you need.</p>
      <div className={styles.actions}>
        <a href="/offers" className="btn-primary" style={{ padding: '0.7rem 1.8rem', borderRadius: 6, background: '#2e7d32', color: '#fff', fontFamily: 'inherit', fontSize: '1rem' }}>Browse Offers</a>
        <a href="/register" className="btn-secondary" style={{ padding: '0.7rem 1.8rem', borderRadius: 6, background: '#fff', color: '#2e7d32', border: '1.5px solid #2e7d32', fontFamily: 'inherit', fontSize: '1rem' }}>Join the Community</a>
      </div>
    </main>
  );
}
