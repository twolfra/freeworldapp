import { useEffect, useState } from 'react';
import { auth } from '../api/client';
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
            <h2>Verifying your email…</h2>
            <p className={styles.sub}>Please wait a moment.</p>
          </>
        )}

        {status === 'success' && (
          <>
            <h2 className={styles.success}>Email verified!</h2>
            <p>Your account is now active. You can sign in.</p>
            <a href="/login" className="btn-primary" style={{ textAlign: 'center' }}>Sign in</a>
          </>
        )}

        {(status === 'error' || status === 'resend') && (
          <>
            <h2>Verify your email</h2>
            {message && <p className={styles.errorMsg}>{message}</p>}
            <p>Enter your email address to receive a new verification link.</p>
            <ResendForm email={resendEmail} setEmail={setResendEmail} status={resendStatus} onSubmit={handleResend} />
          </>
        )}

        {status === 'expired' && (
          <>
            <h2>Link expired</h2>
            <p className={styles.errorMsg}>This verification link has expired.</p>
            <p>Enter your email to receive a new one.</p>
            <ResendForm email={resendEmail} setEmail={setResendEmail} status={resendStatus} onSubmit={handleResend} />
          </>
        )}
      </div>
    </main>
  );
}

function ResendForm({ email, setEmail, status, onSubmit }) {
  if (status === 'sent') {
    return <p className={styles.success}>Verification email sent — check your inbox.</p>;
  }
  return (
    <form onSubmit={onSubmit} className={styles.resend}>
      <input
        type="email"
        placeholder="your@email.com"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
      />
      <button type="submit" className="btn-primary" disabled={status === 'sending'}>
        {status === 'sending' ? 'Sending…' : 'Resend verification email'}
      </button>
      {status === 'error' && <p className={styles.errorMsg}>Something went wrong. Please try again.</p>}
    </form>
  );
}
