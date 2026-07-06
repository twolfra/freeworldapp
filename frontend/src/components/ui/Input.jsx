import { useId } from 'react';
import styles from './Input.module.css';

/** Labeled text input. Props: label, error, id (auto-generated if omitted). */
export default function Input({ label, error, id, className = '', ...rest }) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const errorId = `${inputId}-error`;
  return (
    <div className={[styles.field, className].filter(Boolean).join(' ')}>
      {label && <label className={styles.label} htmlFor={inputId}>{label}</label>}
      <input
        id={inputId}
        className={[styles.control, error ? styles.invalid : ''].filter(Boolean).join(' ')}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...rest}
      />
      {error && <p className={styles.error} id={errorId} role="alert">{error}</p>}
    </div>
  );
}
