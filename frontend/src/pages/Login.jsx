import { useState } from 'react';
import { auth } from '../api/client';
import styles from './Register.module.css';

export default function Login() {
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState(null);
  const [unverified, setUnverified] = useState(false);

  function handleChange(e) {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setUnverified(false);
    try {
      const user = await auth.login(form);
      localStorage.setItem('currentUser', JSON.stringify(user));
      window.location.href = '/offers';
    } catch (err) {
      if (err.message && err.message.toLowerCase().includes('not verified')) {
        setUnverified(true);
      } else {
        setError('Invalid username or password.');
      }
    }
  }

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit}>
        <h2>Welcome back</h2>
        <p className={styles.sub}>Sign in to your FreeWorld account.</p>
        {error && <p className={styles.error}>{error}</p>}
        {unverified && (
          <p className={styles.error}>
            Email not verified.{' '}
            <a href="/verify-email?resend=1" style={{ color: '#c62828' }}>
              Resend verification email
            </a>
          </p>
        )}
        <label>
          Username
          <input name="username" value={form.username} onChange={handleChange} required />
        </label>
        <label>
          Password
          <input name="password" type="password" value={form.password} onChange={handleChange} required />
        </label>
        <button type="submit" className="btn-primary">Sign In</button>
        <p style={{ fontSize: '0.88rem', textAlign: 'center', color: '#666' }}>
          No account yet? <a href="/register" style={{ color: '#2e7d32' }}>Join the community</a>
        </p>
      </form>
    </main>
  );
}
