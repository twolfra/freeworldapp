import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { t } from '../i18n';
import { Button } from '../components/ui';
import { hasOnboarded } from './onboardingFlag';
import styles from './Register.module.css';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState(null);
  const [unverified, setUnverified] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  function handleChange(e) {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setUnverified(false);
    setSubmitting(true);
    try {
      await login(form);
      // First sign-in on this device → run the mini-onboarding once.
      navigate(hasOnboarded() ? '/offers' : '/welcome');
    } catch (err) {
      if (err.message && err.message.toLowerCase().includes('not verified')) {
        setUnverified(true);
      } else {
        setError(t('login.badCreds'));
      }
      setSubmitting(false);
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
            <Link to="/verify-email?resend=1" style={{ color: 'var(--danger)' }}>
              {t('login.resend')}
            </Link>
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
        <p style={{ fontSize: '0.85rem', textAlign: 'right', marginTop: '-0.6rem' }}>
          <Link to="/forgot-password" style={{ color: 'var(--green)' }}>{t('login.forgot')}</Link>
        </p>
        <Button type="submit" loading={submitting}>{t('login.submit')}</Button>
        <p style={{ fontSize: '0.88rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          {t('login.noAccount')} <Link to="/register" style={{ color: 'var(--green)' }}>{t('login.join')}</Link>
        </p>
      </form>
    </main>
  );
}
