import { Link } from 'react-router-dom';
import { t } from '../i18n';
import styles from './Legal.module.css';

export default function NotFound() {
  return (
    <main className={styles.wrap}>
      <div className={styles.inner} style={{ textAlign: 'center', padding: '3rem 0' }}>
        <p style={{ fontSize: '5rem', fontWeight: 800, color: 'var(--green)', margin: 0 }}>404</p>
        <h1>{t('notfound.title')}</h1>
        <p>{t('notfound.text')}</p>
        <p><Link to="/">{t('notfound.home')}</Link></p>
      </div>
    </main>
  );
}
