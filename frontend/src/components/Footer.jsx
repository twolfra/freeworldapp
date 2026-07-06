import { Link } from 'react-router-dom';
import { t } from '../i18n';
import styles from './Footer.module.css';

export default function Footer() {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <span className={styles.copy}>{t('footer.copy')}</span>
        <nav className={styles.links}>
          <Link to="/contact">{t('footer.contact')}</Link>
          <Link to="/impressum">{t('footer.impressum')}</Link>
          <Link to="/datenschutz">{t('footer.datenschutz')}</Link>
          <Link to="/terms">{t('footer.terms')}</Link>
        </nav>
      </div>
    </footer>
  );
}
