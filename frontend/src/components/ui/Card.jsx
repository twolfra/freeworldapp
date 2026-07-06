import styles from './Card.module.css';

/** Elevated surface. Props: as (element, default 'div'), padded (default true). */
export default function Card({ as: Tag = 'div', padded = true, className = '', children, ...rest }) {
  const cls = [styles.card, padded ? styles.padded : '', className].filter(Boolean).join(' ');
  return (
    <Tag className={cls} {...rest}>
      {children}
    </Tag>
  );
}
