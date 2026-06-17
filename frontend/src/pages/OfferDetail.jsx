import { useEffect, useState } from 'react';
import { offers as offersApi, images as imagesApi, likes as likesApi, admin as adminApi } from '../api/client';
import { t, tCat } from '../i18n';
import ReportButton from '../components/ReportButton';
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
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');

  useEffect(() => {
    offersApi.get(id)
      .then((o) => { setOffer(o); document.title = `${o.title} — FreeWorld`; })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    if (currentUser) {
      likesApi.check('offer', id)
        .then((data) => {
          setLiked(data.liked);
          setLikeCount(data.count);
        })
        .catch(() => {});
    }
  }, [id, currentUser]);

  if (loading) return <p className={styles.status}>{t('detail.loading')}</p>;
  if (error)   return <p className={styles.status}>{t('detail.loadErrOffer')}{error}</p>;

  const isOwnPost = currentUser?.id === offer.offeredById;
  const isAdmin = currentUser?.role === 'ADMIN';

  async function handleAdminDelete() {
    if (!window.confirm(t('admin.confirmDelete'))) return;
    setDeleting(true);
    try {
      await adminApi.deleteOffer(id);
      window.location.href = '/offers';
    } catch (err) {
      alert(t('detail.deleteErr') + err.message);
      setDeleting(false);
    }
  }

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
    if (!window.confirm(t('detail.confirmOffer'))) return;
    setDeleting(true);
    try {
      await offersApi.remove(id);
      window.location.href = '/offers';
    } catch (err) {
      alert(t('detail.deleteErr') + err.message);
      setDeleting(false);
    }
  }

  async function toggleLike() {
    if (!currentUser) {
      window.location.href = '/login';
      return;
    }
    try {
      if (liked) {
        await likesApi.unlike('offer', id);
        setLiked(false);
        setLikeCount(c => c - 1);
      } else {
        await likesApi.like('offer', id);
        setLiked(true);
        setLikeCount(c => c + 1);
      }
    } catch (err) {
      console.error(err);
    }
  }

  return (
    <main className={styles.page}>
      <a href="/offers" className={styles.back} style={{ color: '#2e7d32' }}>{t('detail.backOffers')}</a>
      <div className={styles.card}>
        {!editing && offer.imageUrl && <img src={offer.imageUrl} className={styles.image} alt={offer.title} />}
        <span className={styles.category} style={{ color: '#2e7d32' }}>{tCat(offer.category)}</span>
        <h1>{offer.title}</h1>
        <div className={styles.authorRow}>
          <span>{t('detail.postedBy')}</span>
          <a href={`/users/${offer.offeredById}`} className={styles.authorLink}>
            {offer.offeredByUsername}
          </a>
          {!isOwnPost && currentUser && (
            <a href={`/messages/${offer.offeredById}`} className={styles.contactBtn}>
              {t('detail.contact')}
            </a>
          )}
          {!currentUser && (
            <a href="/login" className={styles.contactBtn}>{t('detail.signInContact')}</a>
          )}
          <button
            onClick={toggleLike}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              fontSize: '1rem',
              marginLeft: 'auto',
              color: liked ? '#dc2626' : '#999',
              fontWeight: 'bold',
            }}
            title={liked ? 'Unlike' : 'Like'}
          >
            {liked ? '❤' : '🤍'} {likeCount}
          </button>
          {currentUser && !isOwnPost && (
            <ReportButton targetType="OFFER" targetId={id} />
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
        {isAdmin && !isOwnPost && !editing && (
          <div className={styles.ownerActions}>
            <button className={styles.deleteBtn} onClick={handleAdminDelete} disabled={deleting}>
              {deleting ? t('detail.deleting') : t('admin.deletePostBtn')}
            </button>
          </div>
        )}
        {editing ? (
          <form className={styles.editForm} onSubmit={handleSave}>
            <h3>{t('edit.offer')}</h3>
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
            <p className={styles.description}>{offer.description}</p>
            <dl className={styles.meta}>
              <div>
                <dt>{t('detail.region')}</dt>
                <dd>{offer.region}</dd>
              </div>
              <div>
                <dt>{t('detail.qtyAvail')}</dt>
                <dd>{offer.quantity}</dd>
              </div>
              <div>
                <dt>{t('detail.posted')}</dt>
                <dd>{new Date(offer.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}</dd>
              </div>
            </dl>
          </>
        )}
      </div>
    </main>
  );
}
