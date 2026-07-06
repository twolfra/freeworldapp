import { useId } from 'react';
import styles from './Select.module.css';

/** Labeled select. Props: label, error, id (auto), children = <option>s. */
export default function Select({ label, error, id, className = '', children, ...rest }) {
  const autoId = useId();
  const selectId = id ?? autoId;
  const errorId = `${selectId}-error`;
  return (
    <div className={[styles.field, className].filter(Boolean).join(' ')}>
      {label && <label className={styles.label} htmlFor={selectId}>{label}</label>}
      <select
        id={selectId}
        className={[styles.select, error ? styles.invalid : ''].filter(Boolean).join(' ')}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...rest}
      >
        {children}
      </select>
      {error && <p className={styles.error} id={errorId} role="alert">{error}</p>}
    </div>
  );
}
