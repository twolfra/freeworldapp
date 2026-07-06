import styles from './Skeleton.module.css';

/**
 * Loading placeholder with shimmer.
 * variant="block" (default): one rectangle, sized via width/height props.
 * variant="text": `lines` stacked text-height lines (last one shortened).
 */
export default function Skeleton({ variant = 'block', width, height, lines = 3, className = '', ...rest }) {
  if (variant === 'text') {
    return (
      <span
        className={[styles.textGroup, className].filter(Boolean).join(' ')}
        style={{ width }}
        aria-hidden="true"
        {...rest}
      >
        {Array.from({ length: lines }, (_, i) => (
          <span
            key={i}
            className={[styles.skeleton, styles.line].join(' ')}
            style={lines > 1 && i === lines - 1 ? { width: '60%' } : undefined}
          />
        ))}
      </span>
    );
  }
  return (
    <span
      className={[styles.skeleton, styles.block, className].filter(Boolean).join(' ')}
      style={{ width, height }}
      aria-hidden="true"
      {...rest}
    />
  );
}
