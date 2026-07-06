import { useToast } from './ui';
import { t } from '../i18n';
import { slugify } from '../utils/slugify';

/** Web Share API with clipboard fallback (AP 3.6). */
export default function ShareButton({ title, path }) {
  const toast = useToast();
  const url = `${window.location.origin}${path}/${slugify(title)}`;

  async function share() {
    if (navigator.share) {
      try { await navigator.share({ title: `${title} — FreeWorld`, url }); } catch { /* cancelled */ }
      return;
    }
    try {
      await navigator.clipboard.writeText(url);
      toast.success(t('share.copied'));
    } catch {
      toast.error(t('share.error'));
    }
  }

  return (
    <button type="button" className="btn-ghost" onClick={share} aria-label={t('share.button')}>
      ↗ {t('share.button')}
    </button>
  );
}
