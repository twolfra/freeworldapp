import { useId } from 'react';
import styles from './Textarea.module.css';

/** Labeled textarea. Props: label, error, id (auto), rows (default 4). */
export default function Textarea({ label, error, id, rows = 4, className = '', ...rest }) {
  const autoId = useId();
  const areaId = id ?? autoId;
  const errorId = `${areaId}-error`;
  return (
    <div className={[styles.field, className].filter(Boolean).join(' ')}>
      {label && <label className={styles.label} htmlFor={areaId}>{label}</label>}
      <textarea
        id={areaId}
        rows={rows}
        className={[styles.textarea, error ? styles.invalid : ''].filter(Boolean).join(' ')}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...rest}
      />
      {error && <p className={styles.error} id={errorId} role="alert">{error}</p>}
    </div>
  );
}
