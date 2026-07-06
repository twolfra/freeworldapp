import { useCallback, useEffect, useState } from 'react';
import { t, tp } from '../i18n';
import styles from './Gallery.module.css';

/**
 * Detail-page gallery with a self-built lightbox (AP 3.3).
 * Falls back to the single legacy cover image when there is no gallery.
 */
export default function Gallery({ images, coverUrl, title }) {
  const list = images && images.length > 0
    ? images
    : (coverUrl ? [{ url: coverUrl, thumbUrl: null }] : []);
  const [index, setIndex] = useState(0);
  const [open, setOpen] = useState(false);

  const prev = useCallback(() => setIndex((i) => (i - 1 + list.length) % list.length), [list.length]);
  const next = useCallback(() => setIndex((i) => (i + 1) % list.length), [list.length]);

  useEffect(() => {
    if (!open) return;
    function onKey(e) {
      if (e.key === 'Escape') setOpen(false);
      else if (e.key === 'ArrowLeft') prev();
      else if (e.key === 'ArrowRight') next();
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, prev, next]);

  if (list.length === 0) return null;
  const current = list[Math.min(index, list.length - 1)];

  return (
    <div className={styles.gallery}>
      <button type="button" className={styles.main} onClick={() => setOpen(true)}
              aria-label={t('gallery.open')}>
        <img src={current.url} alt={title} />
      </button>

      {list.length > 1 && (
        <div className={styles.strip}>
          {list.map((img, i) => (
            <button key={img.url} type="button"
                    className={i === index ? styles.stripActive : styles.stripThumb}
                    onClick={() => setIndex(i)}
                    aria-label={tp('gallery.thumb', { n: i + 1 })}>
              <img src={img.thumbUrl || img.url} alt="" />
            </button>
          ))}
        </div>
      )}

      {open && (
        <div className={styles.lightbox} role="dialog" aria-modal="true"
             onClick={() => setOpen(false)}>
          <img src={current.url} alt={title} onClick={(e) => e.stopPropagation()} />
          {list.length > 1 && (
            <>
              <button type="button" className={styles.navPrev}
                      onClick={(e) => { e.stopPropagation(); prev(); }}
                      aria-label={t('gallery.prev')}>‹</button>
              <button type="button" className={styles.navNext}
                      onClick={(e) => { e.stopPropagation(); next(); }}
                      aria-label={t('gallery.next')}>›</button>
              <span className={styles.counter}>{index + 1}/{list.length}</span>
            </>
          )}
          <button type="button" className={styles.close} aria-label={t('ui.close')}
                  onClick={() => setOpen(false)}>✕</button>
        </div>
      )}
    </div>
  );
}
