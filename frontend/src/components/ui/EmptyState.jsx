import styles from './EmptyState.module.css';

/**
 * Friendly empty/zero-data panel.
 * Props: icon (emoji or any node), title, text, action (CTA slot, e.g. a Button).
 */
export default function EmptyState({ icon, title, text, action, className = '', ...rest }) {
  return (
    <div className={[styles.empty, className].filter(Boolean).join(' ')} {...rest}>
      {icon && (
        <div className={styles.icon} aria-hidden="true">
          {icon}
        </div>
      )}
      {title && <h3 className={styles.title}>{title}</h3>}
      {text && <p className={styles.text}>{text}</p>}
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}
