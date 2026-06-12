import { useState } from 'react';
import { users } from '../api/client';
import styles from './Register.module.css';

export default function Register() {
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState(null);
  const [done, setDone] = useState(false);

  function handleChange(e) {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    try {
      await users.create(form);
      setDone(true);
    } catch (err) {
      setError(err.message);
    }
  }

  if (done) return (
    <main className={styles.page}>
      <div className={styles.card}>
        <h2>Welcome to FreeWorld!</h2>
        <p>Your account is ready. <a href="/offers">Browse offers</a> or <a href="/offers/new">make your first offer</a>.</p>
      </div>
    </main>
  );

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit}>
        <h2>Join the Community</h2>
        <p className={styles.sub}>No money. No trade. Just giving.</p>
        {error && <p className={styles.error}>{error}</p>}
        <label>
          Username
          <input name="username" value={form.username} onChange={handleChange} required minLength={3} maxLength={32} />
        </label>
        <label>
          Email
          <input name="email" type="email" value={form.email} onChange={handleChange} required />
        </label>
        <label>
          Password
          <input name="password" type="password" value={form.password} onChange={handleChange} required minLength={6} />
          <span style={{ fontSize: '0.78rem', color: '#888' }}>At least 6 characters</span>
        </label>
        <button type="submit" className="btn-primary">Create Account</button>
      </form>
    </main>
  );
}
