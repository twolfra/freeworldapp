import { useRef, useState } from 'react';
import { images as imagesApi } from '../api/client';
import { useToast, Spinner } from './ui';
import { t, tp } from '../i18n';
import styles from './GalleryPicker.module.css';

export const MAX_IMAGES = 5;

/**
 * Multi-image picker (AP 3.3). Files are uploaded immediately through the
 * hardened image pipeline; `items` is a list of {url, thumbUrl} in gallery
 * order — the first entry is the cover.
 */
export default function GalleryPicker({ items, onChange }) {
  const toast = useToast();
  const fileRef = useRef(null);
  const [uploading, setUploading] = useState(false);

  async function handleFiles(e) {
    const files = Array.from(e.target.files || []);
    e.target.value = '';
    if (!files.length) return;
    const room = MAX_IMAGES - items.length;
    if (room <= 0) {
      toast.error(tp('gallery.max', { max: MAX_IMAGES }));
      return;
    }
    setUploading(true);
    try {
      const added = [];
      for (const file of files.slice(0, room)) {
        const res = await imagesApi.upload(file);
        added.push({ url: res.url, thumbUrl: res.thumbUrl });
      }
      onChange([...items, ...added]);
      if (files.length > room) toast.error(tp('gallery.max', { max: MAX_IMAGES }));
    } catch (err) {
      toast.error(err.message);
    } finally {
      setUploading(false);
    }
  }

  function move(index, delta) {
    const next = [...items];
    const target = index + delta;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  }

  function remove(index) {
    onChange(items.filter((_, i) => i !== index));
  }

  return (
    <div className={styles.picker}>
      <div className={styles.thumbs}>
        {items.map((img, i) => (
          <div key={img.url} className={styles.thumb}>
            <img src={img.thumbUrl || img.url} alt={tp('gallery.thumb', { n: i + 1 })} />
            {i === 0 && <span className={styles.coverTag}>{t('gallery.cover')}</span>}
            <div className={styles.thumbActions}>
              <button type="button" onClick={() => move(i, -1)} disabled={i === 0}
                      aria-label={t('gallery.moveLeft')}>◀</button>
              <button type="button" onClick={() => remove(i)}
                      aria-label={t('gallery.remove')}>✕</button>
              <button type="button" onClick={() => move(i, 1)} disabled={i === items.length - 1}
                      aria-label={t('gallery.moveRight')}>▶</button>
            </div>
          </div>
        ))}
        {items.length < MAX_IMAGES && (
          <button type="button" className={styles.addTile}
                  onClick={() => fileRef.current?.click()} disabled={uploading}>
            {uploading ? <Spinner size="sm" /> : <>＋<br />{t('gallery.add')}</>}
          </button>
        )}
      </div>
      <span className={styles.count}>{tp('gallery.count', { n: items.length, max: MAX_IMAGES })}</span>
      <input ref={fileRef} type="file" accept="image/*" multiple hidden onChange={handleFiles} />
    </div>
  );
}
