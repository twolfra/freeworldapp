import { useState } from 'react';
import { Link } from 'react-router-dom';
import { users } from '../api/client';
import { t, getLang } from '../i18n';
import { Button } from '../components/ui';
import styles from './Register.module.css';

export default function Register() {
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState(null);
  const [registered, setRegistered] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);

  function handleChange(e) {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await users.create({ ...form, language: getLang() });
      setRegisteredEmail(form.email);
      setRegistered(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
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
            <Link to={`/verify-email?resend=1&email=${encodeURIComponent(registeredEmail)}`}>
              {t('register.checkResend')}
            </Link>.
          </p>
          <Link to="/login" className="btn-primary" style={{ textAlign: 'center' }}>{t('register.checkGoLogin')}</Link>
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
          <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{t('register.passwordHint')}</span>
        </label>
        <Button type="submit" loading={submitting}>{t('register.submit')}</Button>
      </form>
    </main>
  );
}
