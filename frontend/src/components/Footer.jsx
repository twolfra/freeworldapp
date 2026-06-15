import { t } from '../i18n';
import styles from './Footer.module.css';

export default function Footer() {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <span className={styles.copy}>{t('footer.copy')}</span>
        <nav className={styles.links}>
          <a href="/contact">{t('footer.contact')}</a>
          <a href="/impressum">{t('footer.impressum')}</a>
          <a href="/datenschutz">{t('footer.datenschutz')}</a>
          <a href="/terms">{t('footer.terms')}</a>
        </nav>
      </div>
    </footer>
  );
}
