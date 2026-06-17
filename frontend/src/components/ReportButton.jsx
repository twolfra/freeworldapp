import { useState } from 'react';
import { reports as reportsApi } from '../api/client';
import { t } from '../i18n';
import styles from './ReportButton.module.css';

const REASONS = ['SPAM', 'INAPPROPRIATE', 'SCAM', 'HARASSMENT', 'OTHER'];

/**
 * Small "Report" link + modal. Works for any target — pass targetType
 * ('OFFER' | 'REQUEST' | 'USER') and the target's id.
 */
export default function ReportButton({ targetType, targetId, className }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState('SPAM');
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState(null);

  async function submit(e) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await reportsApi.create(targetType, targetId, reason, note || null);
      setDone(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <button type="button" className={className || styles.trigger} onClick={() => setOpen(true)}>
        {t('report.button')}
      </button>
      {open && (
        <div className={styles.overlay} onClick={() => setOpen(false)}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            {done ? (
              <>
                <h3>{t('report.thanksTitle')}</h3>
                <p className={styles.sub}>{t('report.thanksBody')}</p>
                <div className={styles.actions}>
                  <button className={styles.submitBtn} onClick={() => setOpen(false)}>{t('report.close')}</button>
                </div>
              </>
            ) : (
              <form onSubmit={submit}>
                <h3>{t('report.title')}</h3>
                <p className={styles.sub}>{t('report.intro')}</p>
                {error && <p className={styles.error}>{error}</p>}
                <label>
                  {t('report.reasonLabel')}
                  <select value={reason} onChange={(e) => setReason(e.target.value)}>
                    {REASONS.map((r) => (
                      <option key={r} value={r}>{t('report.reason.' + r)}</option>
                    ))}
                  </select>
                </label>
                <label>
                  {t('report.noteLabel')}
                  <textarea
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    maxLength={1000}
                    rows={3}
                    placeholder={t('report.notePlaceholder')}
                  />
                </label>
                <div className={styles.actions}>
                  <button type="button" className={styles.cancelBtn} onClick={() => setOpen(false)}>
                    {t('report.cancel')}
                  </button>
                  <button type="submit" className={styles.submitBtn} disabled={submitting}>
                    {submitting ? t('report.submitting') : t('report.submit')}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </>
  );
}
