import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { requests as requestsApi, images as imagesApi, likes as likesApi, admin as adminApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t, tCat } from '../i18n';
import ReportButton from '../components/ReportButton';
import { Button, ConfirmModal, useToast } from '../components/ui';
import styles from './RequestDetail.module.css';

const CATEGORIES = [
  'Food & Drink', 'Clothing', 'Books & Media', 'Tools & Equipment',
  'Furniture', 'Electronics', 'Skills & Services', 'Plants & Seeds',
  'Childcare', 'Transport', 'Other',
];

export default function RequestDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [request, setRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [newImageFile, setNewImageFile] = useState(null);
  const [editError, setEditError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null); // 'delete' | 'adminDelete' | 'fulfilled'
  const [statusSaving, setStatusSaving] = useState(false);
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const { user: currentUser } = useAuth();
  const toast = useToast();

  useEffect(() => {
    requestsApi.get(id)
      .then((r) => { setRequest(r); document.title = `${r.title} — FreeWorld`; })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    if (currentUser) {
      likesApi.check('request', id)
        .then((data) => {
          setLiked(data.liked);
          setLikeCount(data.count);
        })
        .catch(() => {});
    }
  }, [id, currentUser]);

  if (loading) return <p className={styles.status}>{t('detail.loading')}</p>;
  if (error)   return <p className={styles.status}>{t('detail.loadErrRequest')}{error}</p>;

  const isOwnPost = currentUser?.id === request.requestedById;
  const isAdmin = currentUser?.role === 'ADMIN';

  async function handleAdminDelete() {
    try {
      await adminApi.deleteRequest(id);
      navigate('/requests');
    } catch (err) {
      toast.error(t('detail.deleteErr') + err.message);
      setConfirmAction(null);
    }
  }

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
    try {
      await requestsApi.remove(id);
      navigate('/requests');
    } catch (err) {
      toast.error(t('detail.deleteErr') + err.message);
      setConfirmAction(null);
    }
  }

  async function changeStatus(status) {
    setStatusSaving(true);
    try {
      const updated = await requestsApi.setStatus(id, status);
      setRequest(updated);
      toast.success(t('detail.statusUpdated'));
    } catch (err) {
      toast.error(t('detail.statusErr') + err.message);
    } finally {
      setStatusSaving(false);
      setConfirmAction(null);
    }
  }

  async function toggleLike() {
    if (!currentUser) {
      navigate('/login');
      return;
    }
    // Optimistic update: flip immediately, revert on API error.
    const wasLiked = liked;
    setLiked(!wasLiked);
    setLikeCount((c) => c + (wasLiked ? -1 : 1));
    try {
      if (wasLiked) {
        await likesApi.unlike('request', id);
      } else {
        await likesApi.like('request', id);
      }
    } catch (err) {
      setLiked(wasLiked);
      setLikeCount((c) => c + (wasLiked ? 1 : -1));
      toast.error(err.message);
    }
  }

  return (
    <main className={styles.page}>
      <Link to="/requests" className={styles.back}>{t('detail.backRequests')}</Link>
      <div className={styles.card}>
        {request.status === 'FULFILLED' && (
          <div className={styles.completedBanner}>
            <span>{t('detail.fulfilledBanner')}</span>
          </div>
        )}
        {!editing && request.imageUrl && <img src={request.imageUrl} className={styles.image} alt={request.title} />}
        <span className={styles.category}>{tCat(request.category)}</span>
        <h1>{request.title}</h1>
        <div className={styles.authorRow}>
          <span>{t('detail.postedBy')}</span>
          <Link to={`/users/${request.requestedById}`} className={styles.authorLink}>
            {request.requestedByUsername}
          </Link>
          {!isOwnPost && currentUser && (
            <Link to={`/messages/${request.requestedById}`} className={styles.contactBtn}>
              {t('detail.contact')}
            </Link>
          )}
          {!currentUser && (
            <Link to="/login" className={styles.contactBtn}>{t('detail.signInContact')}</Link>
          )}
          <button
            onClick={toggleLike}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              fontSize: '1rem',
              marginLeft: 'auto',
              color: liked ? 'var(--danger)' : 'var(--muted-soft)',
              fontWeight: 'bold',
            }}
            title={liked ? 'Unlike' : 'Like'}
          >
            {liked ? '❤' : '🤍'} {likeCount}
          </button>
          {currentUser && !isOwnPost && (
            <ReportButton targetType="REQUEST" targetId={id} />
          )}
        </div>
        {isOwnPost && !editing && (
          <div className={styles.ownerActions}>
            <button className={styles.editBtn} onClick={startEdit}>{t('detail.edit')}</button>
            <button className={styles.deleteBtn} onClick={() => setConfirmAction('delete')}>
              {t('detail.delete')}
            </button>
          </div>
        )}
        {isOwnPost && !editing && (
          <div className={styles.statusSection}>
            <h3>{t('detail.statusHeading')}</h3>
            <div className={styles.statusButtons}>
              {['OPEN', 'FULFILLED'].map((s) => (
                <Button
                  key={s}
                  size="sm"
                  variant={(request.status ?? 'OPEN') === s ? 'primary' : 'secondary'}
                  disabled={(request.status ?? 'OPEN') === s || statusSaving}
                  onClick={() => (s === 'FULFILLED' ? setConfirmAction('fulfilled') : changeStatus(s))}
                >
                  {t('status.' + s)}
                </Button>
              ))}
            </div>
            <p className={styles.statusHint}>{t('detail.statusHintRequest')}</p>
          </div>
        )}
        {isAdmin && !isOwnPost && !editing && (
          <div className={styles.ownerActions}>
            <button className={styles.deleteBtn} onClick={() => setConfirmAction('adminDelete')}>
              {t('admin.deletePostBtn')}
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
      <ConfirmModal
        open={confirmAction !== null}
        message={
          confirmAction === 'fulfilled' ? t('detail.confirmFulfilled')
          : confirmAction === 'adminDelete' ? t('admin.confirmDelete')
          : t('detail.confirmRequest')
        }
        danger={confirmAction !== 'fulfilled'}
        confirmLabel={confirmAction === 'fulfilled' ? t('status.FULFILLED') : t('detail.delete')}
        onConfirm={
          confirmAction === 'fulfilled' ? () => changeStatus('FULFILLED')
          : confirmAction === 'adminDelete' ? handleAdminDelete
          : handleDelete
        }
        onCancel={() => setConfirmAction(null)}
      />
    </main>
  );
}
