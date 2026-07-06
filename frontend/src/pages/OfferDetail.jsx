import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { offers as offersApi, images as imagesApi, likes as likesApi, admin as adminApi, thanks as thanksApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t, tCat, tp } from '../i18n';
import ReportButton from '../components/ReportButton';
import PostalCodeInput from '../components/PostalCodeInput';
import { Badge, Button, ConfirmModal, Modal, Textarea, useToast } from '../components/ui';
import styles from './RequestDetail.module.css';

const CATEGORIES = [
  'Food & Drink', 'Clothing', 'Books & Media', 'Tools & Equipment',
  'Furniture', 'Electronics', 'Skills & Services', 'Plants & Seeds',
  'Childcare', 'Transport', 'Other',
];

export default function OfferDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [offer, setOffer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState(null);
  const [editPostalText, setEditPostalText] = useState('');
  const [editPostal, setEditPostal] = useState(null); // { plz, city, lat, lon } | null
  const [imagePreview, setImagePreview] = useState(null);
  const [newImageFile, setNewImageFile] = useState(null);
  const [editError, setEditError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null); // 'delete' | 'adminDelete' | 'given'
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [statusSaving, setStatusSaving] = useState(false);
  const [interestedCount, setInterestedCount] = useState(null);
  const [interestSending, setInterestSending] = useState(false);
  const [thanksOpen, setThanksOpen] = useState(false);
  const [thanksText, setThanksText] = useState('');
  const [thanksSending, setThanksSending] = useState(false);
  const { user: currentUser } = useAuth();
  const toast = useToast();

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

  const ownerId = offer?.offeredById;
  useEffect(() => {
    if (!currentUser || !ownerId) return;
    if (currentUser.id !== ownerId && currentUser.role !== 'ADMIN') return;
    offersApi.interestedCount(id)
      .then((data) => setInterestedCount(data.count))
      .catch(() => setInterestedCount(null));
  }, [id, currentUser, ownerId]);

  if (loading) return <p className={styles.status}>{t('detail.loading')}</p>;
  if (error)   return <p className={styles.status}>{t('detail.loadErrOffer')}{error}</p>;

  const isOwnPost = currentUser?.id === offer.offeredById;
  const isAdmin = currentUser?.role === 'ADMIN';

  async function handleAdminDelete() {
    try {
      await adminApi.deleteOffer(id);
      navigate('/offers');
    } catch (err) {
      toast.error(t('detail.deleteErr') + err.message);
      setConfirmAction(null);
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
    // Prefill the location from the resolved PLZ/city; legacy posts fall back
    // to their free-text region (kept as-is unless a new PLZ is picked).
    if (offer.postalCode) {
      setEditPostal({ plz: offer.postalCode, city: offer.city, lat: offer.lat, lon: offer.lon });
      setEditPostalText(`${offer.postalCode} ${offer.city}`);
    } else {
      setEditPostal(null);
      setEditPostalText(offer.region);
    }
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
      const updated = await offersApi.update(id, {
        ...editForm,
        region: editPostal ? `${editPostal.plz} ${editPostal.city}` : editPostalText,
        postalCode: editPostal?.plz ?? null,
        imageUrl,
      });
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
    try {
      await offersApi.remove(id);
      navigate('/offers');
    } catch (err) {
      toast.error(t('detail.deleteErr') + err.message);
      setConfirmAction(null);
    }
  }

  async function changeStatus(status) {
    setStatusSaving(true);
    try {
      const updated = await offersApi.setStatus(id, status);
      setOffer(updated);
      toast.success(t('detail.statusUpdated'));
    } catch (err) {
      toast.error(t('detail.statusErr') + err.message);
    } finally {
      setStatusSaving(false);
      setConfirmAction(null);
    }
  }

  async function handleInterest() {
    setInterestSending(true);
    try {
      const res = await offersApi.interest(id);
      navigate(`/messages/${res.conversationWith}`);
    } catch (err) {
      toast.error(err.message);
      setInterestSending(false);
    }
  }

  async function handleThanksSubmit(e) {
    e.preventDefault();
    setThanksSending(true);
    try {
      await thanksApi.create(id, thanksText.trim() || null);
      setThanksOpen(false);
      setThanksText('');
      toast.success(t('thanks.sent'));
    } catch (err) {
      toast.error(err.message);
    } finally {
      setThanksSending(false);
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
        await likesApi.unlike('offer', id);
      } else {
        await likesApi.like('offer', id);
      }
    } catch (err) {
      setLiked(wasLiked);
      setLikeCount((c) => c + (wasLiked ? 1 : -1));
      toast.error(err.message);
    }
  }

  return (
    <main className={styles.page}>
      <Link to="/offers" className={styles.back} style={{ color: 'var(--green)' }}>{t('detail.backOffers')}</Link>
      <div className={styles.card}>
        {offer.status === 'GIVEN' && (
          <div className={styles.completedBanner}>
            <span>{t('detail.givenBanner')}</span>
            {currentUser && !isOwnPost && (
              <Button size="sm" variant="accent" onClick={() => setThanksOpen(true)}>
                {t('detail.thanksBtn')}
              </Button>
            )}
          </div>
        )}
        {!editing && offer.imageUrl && <img src={offer.imageUrl} className={styles.image} alt={offer.title} />}
        <span className={styles.category} style={{ color: 'var(--green)' }}>{tCat(offer.category)}</span>
        <h1>{offer.title}</h1>
        {offer.status === 'RESERVED' && (
          <div className={styles.statusRow}>
            <Badge variant="warning">
              {offer.reservedForUsername
                ? tp('status.reservedFor', { user: offer.reservedForUsername })
                : t('status.RESERVED')}
            </Badge>
          </div>
        )}
        <div className={styles.authorRow}>
          <span>{t('detail.postedBy')}</span>
          <Link to={`/users/${offer.offeredById}`} className={styles.authorLink}>
            {offer.offeredByUsername}
          </Link>
          {!isOwnPost && currentUser && (
            <>
              <Button
                size="sm"
                onClick={handleInterest}
                loading={interestSending}
                disabled={offer.status === 'GIVEN'}
              >
                {t('detail.interested')}
              </Button>
              <Link to={`/messages/${offer.offeredById}`} className={styles.authorLink}>
                {t('detail.contact')}
              </Link>
            </>
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
            <ReportButton targetType="OFFER" targetId={id} />
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
            <h3>
              {t('detail.statusHeading')}
              {interestedCount != null && (
                <span className={styles.interestedNote}>
                  {tp('detail.interestedCount', { n: interestedCount })}
                </span>
              )}
            </h3>
            <div className={styles.statusButtons}>
              {['ACTIVE', 'RESERVED', 'GIVEN'].map((s) => (
                <Button
                  key={s}
                  size="sm"
                  variant={(offer.status ?? 'ACTIVE') === s ? 'primary' : 'secondary'}
                  disabled={(offer.status ?? 'ACTIVE') === s || statusSaving}
                  onClick={() => (s === 'GIVEN' ? setConfirmAction('given') : changeStatus(s))}
                >
                  {t('status.' + s)}
                </Button>
              ))}
            </div>
            <p className={styles.statusHint}>{t('detail.statusHint')}</p>
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
              {t('form.postal')}
              <PostalCodeInput
                value={editPostalText}
                onChange={(text) => { setEditPostalText(text); setEditPostal(null); }}
                onSelect={(item) => { setEditPostal(item); setEditPostalText(`${item.plz} ${item.city}`); }}
                required
              />
              {editPostal && <span className={styles.resolvedCity}>{tp('form.postalResolved', { city: editPostal.city })}</span>}
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
      <ConfirmModal
        open={confirmAction !== null}
        message={
          confirmAction === 'given' ? t('detail.confirmGiven')
          : confirmAction === 'adminDelete' ? t('admin.confirmDelete')
          : t('detail.confirmOffer')
        }
        danger={confirmAction !== 'given'}
        confirmLabel={confirmAction === 'given' ? t('status.GIVEN') : t('detail.delete')}
        onConfirm={
          confirmAction === 'given' ? () => changeStatus('GIVEN')
          : confirmAction === 'adminDelete' ? handleAdminDelete
          : handleDelete
        }
        onCancel={() => setConfirmAction(null)}
      />
      <Modal open={thanksOpen} onClose={() => setThanksOpen(false)} title={t('thanks.title')}>
        <form onSubmit={handleThanksSubmit}>
          <p className={styles.thanksIntro}>{t('thanks.intro')}</p>
          <Textarea
            label={t('thanks.textLabel')}
            value={thanksText}
            onChange={(e) => setThanksText(e.target.value)}
            maxLength={280}
            rows={3}
            placeholder={t('thanks.placeholder')}
          />
          <p className={styles.charCount}>{thanksText.length}/280</p>
          <div className={styles.thanksActions}>
            <Button type="button" variant="ghost" onClick={() => setThanksOpen(false)}>
              {t('ui.cancel')}
            </Button>
            <Button type="submit" variant="accent" loading={thanksSending}>
              {t('thanks.send')}
            </Button>
          </div>
        </form>
      </Modal>
    </main>
  );
}
