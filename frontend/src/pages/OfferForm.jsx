import { useState } from 'react';
import { offers, images as imagesApi } from '../api/client';
import { t, tCat } from '../i18n';
import styles from './OfferForm.module.css';

const CATEGORIES = [
  'Food & Drink', 'Clothing', 'Books & Media', 'Tools & Equipment',
  'Furniture', 'Electronics', 'Skills & Services', 'Plants & Seeds',
  'Childcare', 'Transport', 'Other',
];

export default function OfferForm() {
  const currentUser = JSON.parse(localStorage.getItem('currentUser') ?? 'null');

  const [form, setForm]           = useState({ title: '', description: '', region: '', category: '', quantity: 1 });
  const [imageFile, setImageFile] = useState(null);
  const [preview, setPreview]     = useState(null);
  const [error, setError]         = useState(null);
  const [done, setDone]           = useState(false);
  const [submitting, setSubmitting] = useState(false);

  if (!currentUser) return (
    <main className={styles.page}>
      <div className={styles.card}>
        <h2>{t('form.signInTitle')}</h2>
        <p>{t('form.signInOffer')} <a href="/login">{t('form.signIn')}</a> or <a href="/register">{t('form.join')}</a>.</p>
      </div>
    </main>
  );

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: name === 'quantity' ? Number(value) : value }));
  }

  function handleImage(e) {
    const file = e.target.files[0];
    if (!file) return;
    setImageFile(file);
    setPreview(URL.createObjectURL(file));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      let imageUrl = null;
      if (imageFile) {
        const res = await imagesApi.upload(imageFile);
        imageUrl = res.url;
      }
      await offers.create({ ...form, offeredById: currentUser.id, imageUrl });
      setDone(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (done) return (
    <main className={styles.page}>
      <div className={styles.card}>
        <h2>{t('offerForm.doneTitle')}</h2>
        <p>{t('offerForm.doneText')} <a href="/offers">{t('offerForm.doneSee')}</a> or <a href="/offers/new">{t('offerForm.doneAnother')}</a>.</p>
      </div>
    </main>
  );

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit}>
        <h2>{t('offerForm.heading')}</h2>
        <p className={styles.sub}>{t('offerForm.as')} <strong>{currentUser.username}</strong></p>
        {error && <p className={styles.error}>{error}</p>}

        <label>
          {t('form.title')}
          <input name="title" value={form.title} onChange={handleChange} required maxLength={140} placeholder={t('offerForm.titlePlaceholder')} />
        </label>

        <label>
          {t('form.desc')}
          <textarea name="description" value={form.description} onChange={handleChange} required maxLength={4000} rows={4} placeholder={t('offerForm.descPlaceholder')} />
        </label>

        <div className={styles.row}>
          <label>
            {t('form.category')}
            <select name="category" value={form.category} onChange={handleChange} required>
              <option value="">{t('form.categoryDefault')}</option>
              {CATEGORIES.map((c) => <option key={c} value={c}>{tCat(c)}</option>)}
            </select>
          </label>
          <label>
            {t('form.qty')}
            <input name="quantity" type="number" value={form.quantity} onChange={handleChange} required min={1} />
          </label>
        </div>

        <label>
          {t('form.region')}
          <input name="region" value={form.region} onChange={handleChange} required maxLength={140} placeholder={t('form.regionPlaceholder')} />
        </label>

        <label className={styles.photoLabel}>
          {t('form.photo')} <span className={styles.optional}>{t('form.photoOptional')}</span>
          <input type="file" accept="image/*" onChange={handleImage} className={styles.fileInput} />
          {preview
            ? <img src={preview} className={styles.preview} alt="Preview" />
            : <div className={styles.photoPlaceholder}>{t('form.photoPlaceholder')}</div>
          }
        </label>

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? t('offerForm.submitting') : t('offerForm.submit')}
        </button>
      </form>
    </main>
  );
}
