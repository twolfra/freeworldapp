import { useState } from 'react';
import { t } from '../i18n';
import { Button } from '../components/ui';
import styles from './Legal.module.css';

export default function Contact() {
  const [status, setStatus] = useState('idle'); // idle | submitting | success | error

  async function handleSubmit(e) {
    e.preventDefault();
    setStatus('submitting');
    const formData = new FormData(e.target);
    formData.append('access_key', '57df2351-3624-4b04-8e7f-6c37668efa72');
    const res = await fetch('https://api.web3forms.com/submit', { method: 'POST', body: formData });
    const data = await res.json();
    setStatus(data.success ? 'success' : 'error');
  }

  return (
    <main className={styles.wrap}>
      <div className={styles.inner}>
        <h1>{t('contact.heading')}</h1>
        <p>{t('contact.intro')}</p>

        {status === 'success' ? (
          <p style={{ color: 'var(--green)', fontWeight: 600, marginTop: '1.5rem' }}>
            {t('contact.success')}
          </p>
        ) : (
          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.field}>
              <label className={styles.label}>{t('contact.name')}</label>
              <input className={styles.input} type="text" name="name" required maxLength={120} />
            </div>
            <div className={styles.field}>
              <label className={styles.label}>{t('contact.email')}</label>
              <input className={styles.input} type="email" name="email" required maxLength={255} />
            </div>
            <div className={styles.field}>
              <label className={styles.label}>{t('contact.message')}</label>
              <textarea
                className={styles.input}
                name="message"
                required
                maxLength={3000}
                rows={6}
                style={{ resize: 'vertical' }}
              />
            </div>
            {status === 'error' && (
              <p className={styles.formError}>{t('contact.error')}</p>
            )}
            <Button
              type="submit"
              loading={status === 'submitting'}
              style={{ alignSelf: 'flex-start' }}
            >
              {status === 'submitting' ? t('contact.submitting') : t('contact.submit')}
            </Button>
          </form>
        )}
      </div>
    </main>
  );
}
