import { useEffect, useState } from 'react';
import { offers as offersApi, images as imagesApi } from '../api/client';
import styles from './RequestDetail.module.css';

const CATEGORIES = [
  'Food & Drink', 'Clothing', 'Books & Media', 'Tools & Equipment',
  'Furniture', 'Electronics', 'Skills & Services', 'Plants & Seeds',
  'Childcare', 'Transport', 'Other',
];

export default function OfferDetail({ id }) {
  const [offer, setOffer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [newImageFile, setNewImageFile] = useState(null);
  const [editError, setEditError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');

  useEffect(() => {
    offersApi.get(id)
      .then(setOffer)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p className={styles.status}>Loading…</p>;
  if (error)   return <p className={styles.status}>Could not load offer: {error}</p>;

  const isOwnPost = currentUser?.id === offer.offeredById;

  function startEdit() {
    setEditForm({
      title: offer.title,
      description: offer.description,
      region: offer.region,
      category: offer.category,
      quantity: offer.quantity,
      imageUrl: offer.imageUrl,
    });
    setImagePreview(offer.imageUrl);
    setNewImageFile(null);
    setEditError(null);
    setEditing(true);
  }

  function handleEditChange(e) {
    const { name, value } = e.target;
    setEditForm((f) => ({ ...f, [name]: name === 'quantity' ? Number(value) : value }));
  }

  function handleNewImage(e) {
    const file = e.target.files[0];
    if (!file) return;
    setNewImageFile(file);
    setImagePreview(URL.createObjectURL(file));
  }

  function removeImage() {
    setNewImageFile(null);
    setImagePreview(null);
    setEditForm((f) => ({ ...f, imageUrl: null }));
  }

  async function handleSave(e) {
    e.preventDefault();
    setEditError(null);
    setSaving(true);
    try {
      let imageUrl = editForm.imageUrl;
      if (newImageFile) {
        const res = await imagesApi.upload(newImageFile);
        imageUrl = res.url;
      }
      const updated = await offersApi.update(id, { ...editForm, imageUrl });
      setOffer(updated);
      setEditing(false);
      setNewImageFile(null);
    } catch (err) {
      setEditError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm('Delete this offer? This cannot be undone.')) return;
    setDeleting(true);
    try {
      await offersApi.remove(id);
      window.location.href = '/offers';
    } catch (err) {
      alert('Could not delete: ' + err.message);
      setDeleting(false);
    }
  }

  return (
    <main className={styles.page}>
      <a href="/offers" className={styles.back} style={{ color: '#2e7d32' }}>← Back to Offers</a>
      <div className={styles.card}>
        {!editing && offer.imageUrl && <img src={offer.imageUrl} className={styles.image} alt={offer.title} />}
        <span className={styles.category} style={{ color: '#2e7d32' }}>{offer.category}</span>
        <h1>{offer.title}</h1>
        <div className={styles.authorRow}>
          <span>Posted by</span>
          <a href={`/users/${offer.offeredById}`} className={styles.authorLink}>
            @{offer.offeredByUsername}
          </a>
          {!isOwnPost && currentUser && (
            <a href={`/messages/${offer.offeredById}`} className={styles.contactBtn}>
              Contact
            </a>
          )}
          {!currentUser && (
            <a href="/login" className={styles.contactBtn}>Sign in to contact</a>
          )}
        </div>
        {isOwnPost && !editing && (
          <div className={styles.ownerActions}>
            <button className={styles.editBtn} onClick={startEdit}>Edit</button>
            <button className={styles.deleteBtn} onClick={handleDelete} disabled={deleting}>
              {deleting ? 'Deleting…' : 'Delete'}
            </button>
          </div>
        )}
        {editing ? (
          <form className={styles.editForm} onSubmit={handleSave}>
            <h3>Edit Offer</h3>
            {editError && <p className={styles.editError}>{editError}</p>}
            <label>
              Title
              <input name="title" value={editForm.title} onChange={handleEditChange} required maxLength={140} />
            </label>
            <label>
              Description
              <textarea name="description" value={editForm.description} onChange={handleEditChange} required maxLength={4000} rows={4} />
            </label>
            <div className={styles.editFormRow}>
              <label>
                Category
                <select name="category" value={editForm.category} onChange={handleEditChange} required>
                  {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              </label>
              <label>
                Quantity
                <input name="quantity" type="number" value={editForm.quantity} onChange={handleEditChange} required min={1} />
              </label>
            </div>
            <label>
              Region / Location
              <input name="region" value={editForm.region} onChange={handleEditChange} required maxLength={140} />
            </label>
            <label>
              Photo
              {imagePreview
                ? <div className={styles.editImagePreview}>
                    <img src={imagePreview} alt="Preview" />
                    <button type="button" className={styles.removeImageBtn} onClick={removeImage}>Remove photo</button>
                  </div>
                : <input type="file" accept="image/*" onChange={handleNewImage} className={styles.fileInput} />
              }
              {imagePreview && <input type="file" accept="image/*" onChange={handleNewImage} className={styles.fileInput} style={{ marginTop: '0.4rem' }} />}
            </label>
            <div className={styles.editFormActions}>
              <button type="submit" className={styles.saveBtn} disabled={saving}>
                {saving ? 'Saving…' : 'Save changes'}
              </button>
              <button type="button" className={styles.cancelBtn} onClick={() => setEditing(false)}>
                Cancel
              </button>
            </div>
          </form>
        ) : (
          <>
            <p className={styles.description}>{offer.description}</p>
            <dl className={styles.meta}>
              <div>
                <dt>Region</dt>
                <dd>{offer.region}</dd>
              </div>
              <div>
                <dt>Quantity available</dt>
                <dd>{offer.quantity}</dd>
              </div>
              <div>
                <dt>Posted</dt>
                <dd>{new Date(offer.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}</dd>
              </div>
            </dl>
          </>
        )}
      </div>
    </main>
  );
}
