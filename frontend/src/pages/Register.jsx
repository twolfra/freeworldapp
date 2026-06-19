import { useState } from 'react';
import { users } from '../api/client';
import { t, getLang } from '../i18n';
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
      await users.create({ ...form, language: getLang() });
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
          <h2>{t('register.checkTitle')}</h2>
          <p>{t('register.checkText')} <strong>{registeredEmail}</strong>.</p>
          <p className={styles.sub}>{t('register.checkHint')}</p>
          <p className={styles.sub}>
            {t('register.checkSpam')}{' '}
            <a href={`/verify-email?resend=1&email=${encodeURIComponent(registeredEmail)}`}>
              {t('register.checkResend')}
            </a>.
          </p>
          <a href="/login" className="btn-primary" style={{ textAlign: 'center' }}>{t('register.checkGoLogin')}</a>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit}>
        <h2>{t('register.heading')}</h2>
        <p className={styles.sub}>{t('register.subtitle')}</p>
        {error && <p className={styles.error}>{error}</p>}
        <label>
          {t('register.username')}
          <input name="username" value={form.username} onChange={handleChange} required minLength={3} maxLength={32} />
        </label>
        <label>
          {t('register.email')}
          <input name="email" type="email" value={form.email} onChange={handleChange} required />
        </label>
        <label>
          {t('register.password')}
          <input name="password" type="password" value={form.password} onChange={handleChange} required minLength={6} />
          <span style={{ fontSize: '0.78rem', color: '#888' }}>{t('register.passwordHint')}</span>
        </label>
        <button type="submit" className="btn-primary">{t('register.submit')}</button>
      </form>
    </main>
  );
}
