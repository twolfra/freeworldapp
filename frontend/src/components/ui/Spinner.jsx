import { t } from '../../i18n';
import styles from './Spinner.module.css';

/** Inline loading indicator. Sizes: sm | md | lg. */
export default function Spinner({ size = 'md', label, className = '', ...rest }) {
  const cls = [styles.spinner, styles[size], className].filter(Boolean).join(' ');
  return <span role="status" aria-label={label ?? t('ui.loading')} className={cls} {...rest} />;
}
