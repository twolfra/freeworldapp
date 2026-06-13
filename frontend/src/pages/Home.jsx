import { useState } from 'react';
import styles from './Home.module.css';

const CATEGORIES = [
  { name: 'Food & Drink',       icon: '🍎' },
  { name: 'Clothing',           icon: '👕' },
  { name: 'Books & Media',      icon: '📚' },
  { name: 'Tools & Equipment',  icon: '🔧' },
  { name: 'Furniture',          icon: '🛋️' },
  { name: 'Electronics',        icon: '💻' },
  { name: 'Skills & Services',  icon: '🤝' },
  { name: 'Plants & Seeds',     icon: '🌱' },
  { name: 'Childcare',          icon: '🧸' },
  { name: 'Transport',          icon: '🚲' },
  { name: 'Other',              icon: '📦' },
];

export default function Home() {
  const [q, setQ] = useState('');

  function search(e) {
    e.preventDefault();
    window.location.href = q.trim()
      ? `/offers?q=${encodeURIComponent(q.trim())}`
      : '/offers';
  }

  return (
    <main>
      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <h1>Everything here is <span>free</span>.</h1>
          <p>A community gift economy — no prices, no trades, no money.
            Offer what you can, find what you need.</p>

          <form className={styles.searchBar} onSubmit={search}>
            <span className={styles.searchIcon}>🔍</span>
            <input
              type="search"
              placeholder="What are you looking for?"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              autoComplete="off"
            />
            <button type="submit" className="btn-primary">Search</button>
          </form>

          <div className={styles.heroCtas}>
            <a href="/offers" className={styles.heroLink}>Browse all offers →</a>
            <a href="/requests" className={styles.heroLink}>See what people need →</a>
          </div>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>Browse by category</h2>
        <div className={styles.catGrid}>
          {CATEGORIES.map((c) => (
            <a key={c.name} href={`/offers?q=${encodeURIComponent(c.name)}`} className={styles.catTile}>
              <span className={styles.catIcon}>{c.icon}</span>
              <span className={styles.catName}>{c.name}</span>
            </a>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <div className={styles.splitCards}>
          <a href="/offers/new" className={`${styles.bigCard} ${styles.give}`}>
            <span className={styles.bigEmoji}>🎁</span>
            <h3>Give something away</h3>
            <p>Got something useful you no longer need? Pass it on to your community.</p>
            <span className={styles.bigCta}>Make an offer →</span>
          </a>
          <a href="/requests/new" className={`${styles.bigCard} ${styles.ask}`}>
            <span className={styles.bigEmoji}>🙋</span>
            <h3>Ask for what you need</h3>
            <p>Looking for something specific? Post a request and let neighbours help.</p>
            <span className={styles.bigCta}>Make a request →</span>
          </a>
        </div>
      </section>
    </main>
  );
}
