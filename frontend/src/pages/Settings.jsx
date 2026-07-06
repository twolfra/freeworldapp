import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { users as usersApi, auth as authApi, images as imagesApi, notifications } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t, getLang, setLang } from '../i18n';
import { Avatar, Button, Input, Select, Textarea, ConfirmModal, useToast } from '../components/ui';
import styles from './Settings.module.css';

const BIO_MAX = 500;

export default function Settings() {
  const { user: currentUser } = useAuth();
  const [tab, setTab] = useState('profile');

  if (!currentUser) {
    return (
      <main className={styles.page}>
        <p className={styles.status}>
          {t('settings.signIn')} <Link to="/login">{t('settings.signInLink')}</Link>
        </p>
      </main>
    );
  }

  const tabs = [
    ['profile', t('settings.tabProfile')],
    ['account', t('settings.tabAccount')],
    ['notifications', t('settings.tabNotifications')],
    ['language', t('settings.tabLanguage')],
  ];

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t('settings.title')}</h1>
      <div className={styles.tabs}>
        {tabs.map(([key, label]) => (
          <button
            key={key}
            className={tab === key ? styles.tabActive : styles.tab}
            onClick={() => setTab(key)}
          >
            {label}
          </button>
        ))}
      </div>
      {tab === 'profile' && <ProfileSection />}
      {tab === 'account' && <AccountSection />}
      {tab === 'notifications' && <NotificationsSection />}
      {tab === 'language' && <LanguageSection />}
    </main>
  );
}

function ProfileSection() {
  const { user, updateUser } = useAuth();
  const toast = useToast();
  const fileRef = useRef(null);

  const [displayName, setDisplayName] = useState(user.displayName ?? '');
  const [bio, setBio]                 = useState(user.bio ?? '');
  const [postalCode, setPostalCode]   = useState(user.postalCode ?? '');
  const [city, setCity]               = useState(user.city ?? '');
  const [saving, setSaving]           = useState(false);
  const [avatarBusy, setAvatarBusy]   = useState(false);

  async function applyProfile(fields) {
    const updated = await usersApi.updateProfile(user.id, fields);
    // Merge only profile fields into the stored user — never overwrite the token.
    updateUser({
      displayName: updated.displayName ?? null,
      bio:         updated.bio ?? null,
      avatarUrl:   updated.avatarUrl ?? null,
      postalCode:  updated.postalCode ?? null,
      city:        updated.city ?? null,
    });
  }

  async function handleAvatarFile(e) {
    const file = e.target.files?.[0];
    e.target.value = ''; // allow re-selecting the same file
    if (!file) return;
    setAvatarBusy(true);
    try {
      const { url } = await imagesApi.upload(file);
      await applyProfile({ avatarUrl: url });
      toast.success(t('settings.avatarUpdated'));
    } catch (err) {
      toast.error(err.message);
    } finally {
      setAvatarBusy(false);
    }
  }

  async function removeAvatar() {
    setAvatarBusy(true);
    try {
      await applyProfile({ avatarUrl: '' });
      toast.success(t('settings.avatarRemoved'));
    } catch (err) {
      toast.error(err.message);
    } finally {
      setAvatarBusy(false);
    }
  }

  async function saveProfile(e) {
    e.preventDefault();
    // Send only fields that actually changed; "" clears a field server-side.
    const diff = {};
    if (displayName.trim() !== (user.displayName ?? '')) diff.displayName = displayName.trim();
    if (bio.trim()         !== (user.bio ?? ''))         diff.bio = bio.trim();
    if (postalCode.trim()  !== (user.postalCode ?? ''))  diff.postalCode = postalCode.trim();
    if (city.trim()        !== (user.city ?? ''))        diff.city = city.trim();
    if (Object.keys(diff).length === 0) {
      toast.info(t('settings.nothingToSave'));
      return;
    }
    setSaving(true);
    try {
      await applyProfile(diff);
      toast.success(t('settings.profileSaved'));
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className={styles.section}>
      <div className={styles.avatarRow}>
        <Avatar src={user.avatarUrl} name={user.displayName || user.username} size="lg" />
        <div className={styles.avatarActions}>
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            className={styles.fileInput}
            onChange={handleAvatarFile}
            aria-label={t('settings.uploadAvatar')}
          />
          <Button
            type="button"
            variant="secondary"
            size="sm"
            loading={avatarBusy}
            onClick={() => fileRef.current?.click()}
          >
            {t('settings.uploadAvatar')}
          </Button>
          {user.avatarUrl && (
            <Button type="button" variant="ghost" size="sm" disabled={avatarBusy} onClick={removeAvatar}>
              {t('settings.removeAvatar')}
            </Button>
          )}
        </div>
      </div>

      <form className={styles.form} onSubmit={saveProfile}>
        <Input
          label={t('settings.displayName')}
          value={displayName}
          maxLength={60}
          placeholder={user.username}
          onChange={(e) => setDisplayName(e.target.value)}
        />
        <div>
          <Textarea
            label={t('settings.bio')}
            value={bio}
            maxLength={BIO_MAX}
            rows={5}
            placeholder={t('settings.bioPlaceholder')}
            onChange={(e) => setBio(e.target.value)}
          />
          <p className={styles.counter}>{bio.length}/{BIO_MAX}</p>
        </div>
        <div className={styles.twoCol}>
          <Input
            label={t('settings.postalCode')}
            value={postalCode}
            maxLength={10}
            onChange={(e) => setPostalCode(e.target.value)}
          />
          <Input
            label={t('settings.city')}
            value={city}
            maxLength={100}
            onChange={(e) => setCity(e.target.value)}
          />
        </div>
        <p className={styles.hint}>{t('settings.postalCodeHint')}</p>
        <div className={styles.formActions}>
          <Button type="submit" loading={saving}>{t('settings.saveProfile')}</Button>
        </div>
      </form>
    </section>
  );
}

function AccountSection() {
  const { user, updateUser, logout } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [username, setUsername] = useState(user.username);
  const [email, setEmail]       = useState(user.email ?? '');
  const [accountSaving, setAccountSaving] = useState(false);

  const [oldPw, setOldPw]         = useState('');
  const [newPw, setNewPw]         = useState('');
  const [confirmPw, setConfirmPw] = useState('');
  const [pwError, setPwError]     = useState(null);
  const [pwSaving, setPwSaving]   = useState(false);

  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting]           = useState(false);

  async function saveAccount(e) {
    e.preventDefault();
    setAccountSaving(true);
    try {
      await usersApi.update(user.id, { username: username.trim(), email: email.trim() });
      updateUser({ username: username.trim(), email: email.trim() });
      toast.success(t('settings.accountSaved'));
    } catch (err) {
      toast.error(err.message);
    } finally {
      setAccountSaving(false);
    }
  }

  async function changePassword(e) {
    e.preventDefault();
    setPwError(null);
    if (newPw.length < 10) { setPwError(t('settings.pwTooShort')); return; }
    if (newPw !== confirmPw) { setPwError(t('settings.pwMismatch')); return; }
    setPwSaving(true);
    try {
      await authApi.changePassword(oldPw, newPw);
      setOldPw(''); setNewPw(''); setConfirmPw('');
      toast.success(t('settings.pwChanged'));
    } catch (err) {
      toast.error(err.status === 403 ? t('settings.pwWrongOld') : err.message);
    } finally {
      setPwSaving(false);
    }
  }

  async function deleteAccount() {
    setDeleting(true);
    try {
      await usersApi.remove(user.id);
      setConfirmDelete(false);
      // The server already deleted all sessions along with the account.
      await logout({ skipServer: true });
      navigate('/');
      toast.success(t('settings.deleted'));
    } catch (err) {
      toast.error(err.message);
      setDeleting(false);
    }
  }

  return (
    <section className={styles.section}>
      <form className={styles.form} onSubmit={saveAccount}>
        <h2 className={styles.subheading}>{t('settings.accountHeading')}</h2>
        <div className={styles.twoCol}>
          <Input
            label={t('settings.username')}
            value={username}
            maxLength={32}
            required
            onChange={(e) => setUsername(e.target.value)}
          />
          <Input
            label={t('settings.email')}
            type="email"
            value={email}
            required
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <div className={styles.formActions}>
          <Button type="submit" loading={accountSaving}>{t('settings.saveAccount')}</Button>
        </div>
      </form>

      <form className={styles.form} onSubmit={changePassword}>
        <h2 className={styles.subheading}>{t('settings.pwHeading')}</h2>
        <Input
          label={t('settings.pwOld')}
          type="password"
          autoComplete="current-password"
          value={oldPw}
          required
          onChange={(e) => setOldPw(e.target.value)}
        />
        <div className={styles.twoCol}>
          <Input
            label={t('settings.pwNew')}
            type="password"
            autoComplete="new-password"
            value={newPw}
            required
            onChange={(e) => setNewPw(e.target.value)}
          />
          <Input
            label={t('settings.pwConfirm')}
            type="password"
            autoComplete="new-password"
            value={confirmPw}
            required
            error={pwError}
            onChange={(e) => setConfirmPw(e.target.value)}
          />
        </div>
        <p className={styles.hint}>{t('settings.pwHint')}</p>
        <div className={styles.formActions}>
          <Button type="submit" loading={pwSaving}>{t('settings.pwSubmit')}</Button>
        </div>
      </form>

      <div className={styles.dangerZone}>
        <h2 className={styles.dangerHeading}>{t('settings.dangerHeading')}</h2>
        <p className={styles.hint}>{t('settings.deleteHint')}</p>
        <Button variant="danger" onClick={() => setConfirmDelete(true)}>
          {t('settings.deleteBtn')}
        </Button>
      </div>

      <ConfirmModal
        open={confirmDelete}
        danger
        title={t('settings.deleteConfirmTitle')}
        message={t('settings.deleteConfirmText')}
        confirmLabel={t('settings.deleteConfirmBtn')}
        onConfirm={deleteAccount}
        onCancel={deleting ? undefined : () => setConfirmDelete(false)}
      />
    </section>
  );
}

function NotificationsSection() {
  const { user, updateUser } = useAuth();
  const toast = useToast();
  const [notifyOnMessage, setNotifyOnMessage] = useState(user.notifyOnMessage ?? true);
  const [saving, setSaving] = useState(false);

  async function toggleNotify() {
    const next = !notifyOnMessage;
    setSaving(true);
    try {
      await notifications.updatePreferences({ notifyOnMessage: next });
      setNotifyOnMessage(next);
      // Keep the stored user in sync so the toggle survives reloads.
      updateUser({ notifyOnMessage: next });
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className={styles.section}>
      <h2 className={styles.subheading}>{t('settings.tabNotifications')}</h2>
      <label className={styles.notifyRow}>
        <input type="checkbox" checked={notifyOnMessage} onChange={toggleNotify} disabled={saving} />
        <span>{t('profile.notifyMessages')}</span>
      </label>
      <p className={styles.hint}>{t('profile.notifyMessagesHint')}</p>
    </section>
  );
}

function LanguageSection() {
  const toast = useToast();
  const [lang, setLangValue] = useState(getLang());
  const [saving, setSaving]  = useState(false);

  async function changeLanguage(e) {
    const next = e.target.value;
    setLangValue(next);
    if (next === getLang()) return;
    setSaving(true);
    try {
      // Persist the preference (used for notification emails), then reload in the new language.
      await notifications.updatePreferences({ language: next });
      setLang(next); // reloads the page
    } catch (err) {
      toast.error(err.message);
      setLangValue(getLang());
      setSaving(false);
    }
  }

  return (
    <section className={styles.section}>
      <h2 className={styles.subheading}>{t('settings.tabLanguage')}</h2>
      <div className={styles.langSelect}>
        <Select label={t('settings.language')} value={lang} disabled={saving} onChange={changeLanguage}>
          <option value="en">English</option>
          <option value="de">Deutsch</option>
        </Select>
      </div>
      <p className={styles.hint}>{t('settings.languageHint')}</p>
    </section>
  );
}
