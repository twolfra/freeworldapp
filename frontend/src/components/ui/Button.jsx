import Spinner from './Spinner';
import styles from './Button.module.css';

/**
 * Pill button matching the app's .btn-* look.
 * Props: variant = primary | accent | secondary | ghost | danger,
 * size = sm | md, loading (shows spinner + disables), as (element/component
 * to render, default 'button'), plus anything spread onto the element.
 */
export default function Button({
  as: Tag = 'button',
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  className = '',
  children,
  ...rest
}) {
  const cls = [styles.btn, styles[variant], styles[size], className].filter(Boolean).join(' ');
  return (
    <Tag
      className={cls}
      disabled={Tag === 'button' ? disabled || loading : undefined}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading && <Spinner size="sm" />}
      {children}
    </Tag>
  );
}
