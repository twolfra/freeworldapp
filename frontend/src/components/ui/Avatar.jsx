import styles from './Avatar.module.css';

/**
 * User avatar: image when `src` is set, otherwise the first letter of `name`.
 * Sizes: sm (28px) | md (40px) | lg (64px).
 */
export default function Avatar({ src, name = '', size = 'md', className = '', ...rest }) {
  const cls = [styles.avatar, styles[size], className].filter(Boolean).join(' ');
  if (src) {
    return <img src={src} alt={name} className={cls} {...rest} />;
  }
  const initial = name.trim().charAt(0).toUpperCase() || '?';
  return (
    <span className={cls} role="img" aria-label={name || undefined} {...rest}>
      {initial}
    </span>
  );
}
