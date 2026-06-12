import { useState } from 'react';
import { offers, images as imagesApi } from '../api/client';
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
        <h2>Sign in first</h2>
        <p>You need an account to make an offer. <a href="/login">Sign in</a> or <a href="/register">join</a>.</p>
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
        <h2>Offer posted!</h2>
        <p>Thank you for giving. <a href="/offers">See all offers</a> or <a href="/offers/new">post another</a>.</p>
      </div>
    </main>
  );

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit}>
        <h2>Make an Offer</h2>
        <p className={styles.sub}>Offering as <strong>{currentUser.username}</strong></p>
        {error && <p className={styles.error}>{error}</p>}

        <label>
          Title
          <input name="title" value={form.title} onChange={handleChange} required maxLength={140} placeholder="What are you offering?" />
        </label>

        <label>
          Description
          <textarea name="description" value={form.description} onChange={handleChange} required maxLength={4000} rows={4} placeholder="Tell people more about it — condition, size, how to pick up, etc." />
        </label>

        <div className={styles.row}>
          <label>
            Category
            <select name="category" value={form.category} onChange={handleChange} required>
              <option value="">Select…</option>
              {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </label>
          <label>
            Quantity
            <input name="quantity" type="number" value={form.quantity} onChange={handleChange} required min={1} />
          </label>
        </div>

        <label>
          Region / Location
          <input name="region" value={form.region} onChange={handleChange} required maxLength={140} placeholder="e.g. Berlin, Online, North London" />
        </label>

        <label className={styles.photoLabel}>
          Photo <span className={styles.optional}>(optional)</span>
          <input type="file" accept="image/*" onChange={handleImage} className={styles.fileInput} />
          {preview
            ? <img src={preview} className={styles.preview} alt="Preview" />
            : <div className={styles.photoPlaceholder}>📷 Click to add a photo</div>
          }
        </label>

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Posting…' : 'Post Offer'}
        </button>
      </form>
    </main>
  );
}
