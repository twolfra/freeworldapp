import { useState } from 'react';
import { auth } from '../api/client';
import { t } from '../i18n';
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
        setError(t('login.badCreds'));
      }
    }
  }

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit}>
        <h2>{t('login.heading')}</h2>
        <p className={styles.sub}>{t('login.subtitle')}</p>
        {error && <p className={styles.error}>{error}</p>}
        {unverified && (
          <p className={styles.error}>
            {t('login.unverified')}{' '}
            <a href="/verify-email?resend=1" style={{ color: '#c62828' }}>
              {t('login.resend')}
            </a>
          </p>
        )}
        <label>
          {t('login.username')}
          <input name="username" value={form.username} onChange={handleChange} required />
        </label>
        <label>
          {t('login.password')}
          <input name="password" type="password" value={form.password} onChange={handleChange} required />
        </label>
        <button type="submit" className="btn-primary">{t('login.submit')}</button>
        <p style={{ fontSize: '0.88rem', textAlign: 'center', color: '#666' }}>
          {t('login.noAccount')} <a href="/register" style={{ color: '#2e7d32' }}>{t('login.join')}</a>
        </p>
      </form>
    </main>
  );
}
