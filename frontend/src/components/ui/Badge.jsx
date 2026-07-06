import styles from './Badge.module.css';

/** Small status pill. Variants: neutral | success | danger | warning | accent | blue. */
export default function Badge({ variant = 'neutral', className = '', children, ...rest }) {
  const cls = [styles.badge, styles[variant], className].filter(Boolean).join(' ');
  return (
    <span className={cls} {...rest}>
      {children}
    </span>
  );
}
