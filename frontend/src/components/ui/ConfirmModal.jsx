import { useState } from 'react';
import Modal from './Modal';
import Button from './Button';
import { t } from '../../i18n';
import styles from './ConfirmModal.module.css';

/**
 * Confirmation dialog built on Modal.
 * Props: open, title, message, confirmLabel, cancelLabel, danger,
 * onConfirm (may return a promise — the confirm button shows a spinner
 * until it settles), onCancel.
 */
export default function ConfirmModal({
  open,
  title,
  message,
  confirmLabel,
  cancelLabel,
  danger = false,
  onConfirm,
  onCancel,
}) {
  const [busy, setBusy] = useState(false);

  async function confirm() {
    setBusy(true);
    try {
      await onConfirm?.(); // plain callbacks settle immediately; promises are awaited
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal open={open} onClose={busy ? undefined : onCancel} title={title}>
      {message && <p className={styles.message}>{message}</p>}
      <div className={styles.actions}>
        <Button variant="ghost" onClick={onCancel} disabled={busy}>
          {cancelLabel ?? t('ui.cancel')}
        </Button>
        <Button variant={danger ? 'danger' : 'primary'} onClick={confirm} loading={busy}>
          {confirmLabel ?? t('ui.confirm')}
        </Button>
      </div>
    </Modal>
  );
}
