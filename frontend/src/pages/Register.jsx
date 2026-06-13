import { useState } from 'react';
import { users, auth } from '../api/client';
import styles from './Register.module.css';

export default function Register() {
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState(null);

  function handleChange(e) {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    try {
      await users.create(form);
      try {
        const loggedIn = await auth.login({ username: form.username, password: form.password });
        localStorage.setItem('currentUser', JSON.stringify(loggedIn));
        window.location.href = '/offers';
      } catch {
        window.location.href = '/login';
      }
    } catch (err) {
      setError(err.message);
    }
  }

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
