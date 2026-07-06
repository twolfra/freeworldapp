import { useState } from 'react';
import { Link } from 'react-router-dom';
import { auth } from '../api/client';
import { t } from '../i18n';
import styles from './Register.module.css';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState('form'); // form | sending | sent

  async function handleSubmit(e) {
    e.preventDefault();
    setStatus('sending');
    try {
      await auth.forgotPassword(email);
    } catch {
      // Deliberately ignored: the panel below is always the same neutral
      // message so the form can't be used to probe which emails exist.
    }
    setStatus('sent');
  }

  if (status === 'sent') {
    return (
      <main className={styles.page}>
        <div className={styles.card}>
          <h2>{t('forgot.sentTitle')}</h2>
          <p>{t('forgot.sentText')}</p>
          <Link to="/login" className="btn-primary" style={{ textAlign: 'center' }}>{t('forgot.backToLogin')}</Link>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit}>
        <h2>{t('forgot.heading')}</h2>
        <p className={styles.sub}>{t('forgot.subtitle')}</p>
        <label>
          {t('forgot.email')}
          <input
            name="email"
            type="email"
            placeholder={t('verify.placeholder')}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <button type="submit" className="btn-primary" disabled={status === 'sending'}>
          {status === 'sending' ? t('forgot.sending') : t('forgot.submit')}
        </button>
        <p style={{ fontSize: '0.88rem', textAlign: 'center', color: '#666' }}>
          <Link to="/login" style={{ color: '#2e7d32' }}>{t('forgot.backToLogin')}</Link>
        </p>
      </form>
    </main>
  );
}
