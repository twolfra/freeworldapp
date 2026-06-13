import { useEffect, useState } from 'react';
import { auth } from '../api/client';
import { t } from '../i18n';
import styles from './VerifyEmail.module.css';

export default function VerifyEmail() {
  const params = new URLSearchParams(window.location.search);
  const token = params.get('token');
  const prefilledEmail = params.get('email') || '';
  const resendMode = !token && (params.get('resend') === '1' || prefilledEmail);

  const [status, setStatus] = useState(resendMode ? 'resend' : 'verifying');
  const [message, setMessage] = useState('');
  const [resendEmail, setResendEmail] = useState(prefilledEmail);
  const [resendStatus, setResendStatus] = useState(null);

  useEffect(() => {
    if (!token) return;
    auth.verify(token)
      .then(() => setStatus('success'))
      .catch((err) => {
        const msg = err.message || '';
        setMessage(msg);
        setStatus(msg.toLowerCase().includes('expired') ? 'expired' : 'error');
      });
  }, [token]);

  async function handleResend(e) {
    e.preventDefault();
    setResendStatus('sending');
    try {
      await auth.resendVerification(resendEmail);
      setResendStatus('sent');
    } catch {
      setResendStatus('error');
    }
  }

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        {status === 'verifying' && (
          <>
            <h2>{t('verify.verifying')}</h2>
            <p className={styles.sub}>{t('verify.verifyingHint')}</p>
          </>
        )}

        {status === 'success' && (
          <>
            <h2 className={styles.success}>{t('verify.success')}</h2>
            <p>{t('verify.successText')}</p>
            <a href="/login" className="btn-primary" style={{ textAlign: 'center' }}>{t('verify.signIn')}</a>
          </>
        )}

        {(status === 'error' || status === 'resend') && (
          <>
            <h2>{t('verify.resendHeading')}</h2>
            {message && <p className={styles.errorMsg}>{message}</p>}
            <p>{t('verify.resendHint')}</p>
            <ResendForm email={resendEmail} setEmail={setResendEmail} status={resendStatus} onSubmit={handleResend} />
          </>
        )}

        {status === 'expired' && (
          <>
            <h2>{t('verify.expired')}</h2>
            <p className={styles.errorMsg}>{t('verify.expiredText')}</p>
            <p>{t('verify.expiredHint')}</p>
            <ResendForm email={resendEmail} setEmail={setResendEmail} status={resendStatus} onSubmit={handleResend} />
          </>
        )}
      </div>
    </main>
  );
}

function ResendForm({ email, setEmail, status, onSubmit }) {
  if (status === 'sent') {
    return <p className={styles.success}>{t('verify.sent')}</p>;
  }
  return (
    <form onSubmit={onSubmit} className={styles.resend}>
      <input
        type="email"
        placeholder={t('verify.placeholder')}
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
      />
      <button type="submit" className="btn-primary" disabled={status === 'sending'}>
        {status === 'sending' ? t('verify.sending') : t('verify.resendBtn')}
      </button>
      {status === 'error' && <p className={styles.errorMsg}>{t('verify.error')}</p>}
    </form>
  );
}
