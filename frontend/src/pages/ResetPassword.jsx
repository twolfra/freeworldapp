import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { auth } from '../api/client';
import { t } from '../i18n';
import { Button } from '../components/ui';
import styles from './Register.module.css';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token');

  const [form, setForm] = useState({ password: '', confirm: '' });
  const [error, setError] = useState(null);
  // form | submitting | success | invalid | expired
  const [status, setStatus] = useState(token ? 'form' : 'invalid');

  function handleChange(e) {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    if (form.password.length < 10) {
      setError(t('reset.tooShort'));
      return;
    }
    if (form.password !== form.confirm) {
      setError(t('reset.mismatch'));
      return;
    }
    setStatus('submitting');
    try {
      await auth.resetPassword(token, form.password);
      setStatus('success');
    } catch (err) {
      if (err.status === 410) {
        setStatus('expired');
      } else if (err.status === 404) {
        setStatus('invalid');
      } else {
        // 400 (password policy) or anything unexpected: stay on the form.
        setError(err.status === 400 ? t('reset.tooShort') : err.message);
        setStatus('form');
      }
    }
  }

  if (status === 'success') {
    return (
      <main className={styles.page}>
        <div className={styles.card}>
          <h2>{t('reset.successTitle')}</h2>
          <p>{t('reset.successText')}</p>
          <Link to="/login" className="btn-primary" style={{ textAlign: 'center' }}>{t('reset.signIn')}</Link>
        </div>
      </main>
    );
  }

  if (status === 'invalid' || status === 'expired') {
    return (
      <main className={styles.page}>
        <div className={styles.card}>
          <h2>{status === 'expired' ? t('reset.expiredTitle') : t('reset.invalidTitle')}</h2>
          <p className={styles.error}>
            {status === 'expired' ? t('reset.expiredText') : t('reset.invalidText')}
          </p>
          <Link to="/forgot-password" className="btn-primary" style={{ textAlign: 'center' }}>
            {t('reset.requestNew')}
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit}>
        <h2>{t('reset.heading')}</h2>
        <p className={styles.sub}>{t('reset.subtitle')}</p>
        {error && <p className={styles.error}>{error}</p>}
        <label>
          {t('reset.password')}
          <input name="password" type="password" value={form.password} onChange={handleChange} required minLength={10} />
          <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{t('reset.passwordHint')}</span>
        </label>
        <label>
          {t('reset.confirm')}
          <input name="confirm" type="password" value={form.confirm} onChange={handleChange} required />
        </label>
        <Button type="submit" loading={status === 'submitting'}>
          {status === 'submitting' ? t('reset.submitting') : t('reset.submit')}
        </Button>
      </form>
    </main>
  );
}
