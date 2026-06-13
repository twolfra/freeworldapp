import { useState } from 'react';
import { users } from '../api/client';
import styles from './Register.module.css';

export default function Register() {
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState(null);
  const [registered, setRegistered] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState('');

  function handleChange(e) {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    try {
      await users.create(form);
      setRegisteredEmail(form.email);
      setRegistered(true);
    } catch (err) {
      setError(err.message);
    }
  }

  if (registered) {
    return (
      <main className={styles.page}>
        <div className={styles.card}>
          <h2>Check your inbox</h2>
          <p>We sent a verification link to <strong>{registeredEmail}</strong>.</p>
          <p className={styles.sub}>
            Click the link in the email to activate your account, then sign in.
          </p>
          <p className={styles.sub}>
            Didn't get it? Check your spam folder or{' '}
            <a href={`/verify-email?resend=1&email=${encodeURIComponent(registeredEmail)}`}>
              request a new link
            </a>.
          </p>
          <a href="/login" className="btn-primary" style={{ textAlign: 'center' }}>Go to sign in</a>
        </div>
      </main>
    );
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
