import { useEffect, useState } from 'react';
import { requests as requestsApi, images as imagesApi } from '../api/client';
import { t, tCat } from '../i18n';
import styles from './RequestDetail.module.css';

const CATEGORIES = [
  'Food & Drink', 'Clothing', 'Books & Media', 'Tools & Equipment',
  'Furniture', 'Electronics', 'Skills & Services', 'Plants & Seeds',
  'Childcare', 'Transport', 'Other',
];

export default function RequestDetail({ id }) {
  const [request, setRequest] = useState(null);
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
    requestsApi.get(id)
      .then(setRequest)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p className={styles.status}>{t('detail.loading')}</p>;
  if (error)   return <p className={styles.status}>{t('detail.loadErrRequest')}{error}</p>;

  const isOwnPost = currentUser?.id === request.requestedById;

  function startEdit() {
    setEditForm({
      title: request.title,
      description: request.description,
      region: request.region,
      category: request.category,
      quantity: request.quantity,
      imageUrl: request.imageUrl,
    });
    setImagePreview(request.imageUrl);
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
      const updated = await requestsApi.update(id, { ...editForm, imageUrl });
      setRequest(updated);
      setEditing(false);
      setNewImageFile(null);
    } catch (err) {
      setEditError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm(t('detail.confirmRequest'))) return;
    setDeleting(true);
    try {
      await requestsApi.remove(id);
      window.location.href = '/requests';
    } catch (err) {
      alert(t('detail.deleteErr') + err.message);
      setDeleting(false);
    }
  }

  return (
    <main className={styles.page}>
      <a href="/requests" className={styles.back}>{t('detail.backRequests')}</a>
      <div className={styles.card}>
        {!editing && request.imageUrl && <img src={request.imageUrl} className={styles.image} alt={request.title} />}
        <span className={styles.category}>{tCat(request.category)}</span>
        <h1>{request.title}</h1>
        <div className={styles.authorRow}>
          <span>{t('detail.postedBy')}</span>
          <a href={`/users/${request.requestedById}`} className={styles.authorLink}>
            @{request.requestedByUsername}
          </a>
          {!isOwnPost && currentUser && (
            <a href={`/messages/${request.requestedById}`} className={styles.contactBtn}>
              {t('detail.contact')}
            </a>
          )}
          {!currentUser && (
            <a href="/login" className={styles.contactBtn}>{t('detail.signInContact')}</a>
          )}
        </div>
        {isOwnPost && !editing && (
          <div className={styles.ownerActions}>
            <button className={styles.editBtn} onClick={startEdit}>{t('detail.edit')}</button>
            <button className={styles.deleteBtn} onClick={handleDelete} disabled={deleting}>
              {deleting ? t('detail.deleting') : t('detail.delete')}
            </button>
          </div>
        )}
        {editing ? (
          <form className={styles.editForm} onSubmit={handleSave}>
            <h3>{t('edit.request')}</h3>
            {editError && <p className={styles.editError}>{editError}</p>}
            <label>
              {t('edit.title')}
              <input name="title" value={editForm.title} onChange={handleEditChange} required maxLength={140} />
            </label>
            <label>
              {t('edit.desc')}
              <textarea name="description" value={editForm.description} onChange={handleEditChange} required maxLength={4000} rows={4} />
            </label>
            <div className={styles.editFormRow}>
              <label>
                {t('edit.category')}
                <select name="category" value={editForm.category} onChange={handleEditChange} required>
                  {CATEGORIES.map((c) => <option key={c} value={c}>{tCat(c)}</option>)}
                </select>
              </label>
              <label>
                {t('edit.qty')}
                <input name="quantity" type="number" value={editForm.quantity} onChange={handleEditChange} required min={1} />
              </label>
            </div>
            <label>
              {t('edit.region')}
              <input name="region" value={editForm.region} onChange={handleEditChange} required maxLength={140} />
            </label>
            <label>
              {t('edit.photo')}
              {imagePreview
                ? <div className={styles.editImagePreview}>
                    <img src={imagePreview} alt="Preview" />
                    <button type="button" className={styles.removeImageBtn} onClick={removeImage}>{t('edit.removePhoto')}</button>
                  </div>
                : <input type="file" accept="image/*" onChange={handleNewImage} className={styles.fileInput} />
              }
              {imagePreview && <input type="file" accept="image/*" onChange={handleNewImage} className={styles.fileInput} style={{ marginTop: '0.4rem' }} />}
            </label>
            <div className={styles.editFormActions}>
              <button type="submit" className={styles.saveBtn} disabled={saving}>
                {saving ? t('edit.saving') : t('edit.save')}
              </button>
              <button type="button" className={styles.cancelBtn} onClick={() => setEditing(false)}>
                {t('edit.cancel')}
              </button>
            </div>
          </form>
        ) : (
          <>
            <p className={styles.description}>{request.description}</p>
            <dl className={styles.meta}>
              <div>
                <dt>{t('detail.region')}</dt>
                <dd>{request.region}</dd>
              </div>
              <div>
                <dt>{t('detail.qtyNeeded')}</dt>
                <dd>{request.quantity}</dd>
              </div>
              <div>
                <dt>{t('detail.posted')}</dt>
                <dd>{new Date(request.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}</dd>
              </div>
            </dl>
          </>
        )}
      </div>
    </main>
  );
}
