import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { users as usersApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t, tp, tCat, CATEGORY_LABELS } from '../i18n';
import { Button, Input, useToast } from '../components/ui';
import { markOnboarded } from './onboardingFlag';
import styles from './Onboarding.module.css';

const CATEGORIES = Object.keys(CATEGORY_LABELS);
const TOTAL_STEPS = 3;

/**
 * Post-registration mini-onboarding (/welcome). Three skippable steps:
 * location, category interests, get-started CTAs. Completing or skipping
 * sets the fw_onboarded flag so Login.jsx sends returning users straight
 * to /offers.
 */
export default function Onboarding() {
  const { user, updateUser } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  const [step, setStep] = useState(1);
  const [city, setCity] = useState(user?.city ?? '');
  const [postalCode, setPostalCode] = useState(user?.postalCode ?? '');
  const [selected, setSelected] = useState([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!user) navigate('/login', { replace: true });
  }, [user, navigate]);

  if (!user) return null;

  function finish(to) {
    markOnboarded();
    navigate(to);
  }

  async function handleStep1Next() {
    // Save only what the user actually entered/changed; the step stays optional.
    const diff = {};
    if (city.trim() && city.trim() !== (user.city ?? '')) diff.city = city.trim();
    if (postalCode.trim() && postalCode.trim() !== (user.postalCode ?? '')) diff.postalCode = postalCode.trim();
    if (Object.keys(diff).length > 0) {
      setSaving(true);
      try {
        const updated = await usersApi.updateProfile(user.id, diff);
        updateUser({ city: updated.city ?? null, postalCode: updated.postalCode ?? null });
      } catch {
        // Never block onboarding on a failed save — it can be redone in Settings.
        toast.error(t('onboarding.saveFailed'));
      } finally {
        setSaving(false);
      }
    }
    setStep(2);
  }

  function toggleCategory(cat) {
    setSelected((prev) => (prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat]));
  }

  // The offers list seeds its search from ?q=, so a single picked category
  // makes "Browse offers" land on a pre-filtered list.
  const browseTo = selected.length === 1 ? `/offers?q=${encodeURIComponent(selected[0])}` : '/offers';

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <header className={styles.header}>
          <span className={styles.stepLabel}>{tp('onboarding.stepOf', { n: step, total: TOTAL_STEPS })}</span>
          <button type="button" className={styles.skip} onClick={() => finish('/offers')}>
            {t('onboarding.skip')}
          </button>
        </header>
        <div className={styles.progress} aria-hidden="true">
          {Array.from({ length: TOTAL_STEPS }, (_, i) => (
            <span key={i} className={i + 1 <= step ? `${styles.dot} ${styles.dotActive}` : styles.dot} />
          ))}
        </div>
        <h2 className={styles.welcome}>{t('onboarding.welcome')}</h2>

        {step === 1 && (
          <section className={styles.step}>
            <h3 className={styles.stepTitle}>{t('onboarding.step1Title')}</h3>
            <p className={styles.text}>{t('onboarding.step1Text')}</p>
            <Input
              label={t('settings.city')}
              value={city}
              onChange={(e) => setCity(e.target.value)}
              autoComplete="address-level2"
            />
            <Input
              label={t('settings.postalCode')}
              value={postalCode}
              onChange={(e) => setPostalCode(e.target.value)}
              autoComplete="postal-code"
            />
            <div className={styles.nav}>
              <Button onClick={handleStep1Next} loading={saving}>{t('onboarding.next')}</Button>
            </div>
          </section>
        )}

        {step === 2 && (
          <section className={styles.step}>
            <h3 className={styles.stepTitle}>{t('onboarding.step2Title')}</h3>
            <p className={styles.text}>{t('onboarding.step2Text')}</p>
            <div className={styles.chips}>
              {CATEGORIES.map((c) => (
                <button
                  key={c}
                  type="button"
                  className={selected.includes(c) ? `${styles.chip} ${styles.chipActive}` : styles.chip}
                  aria-pressed={selected.includes(c)}
                  onClick={() => toggleCategory(c)}
                >
                  {tCat(c)}
                </button>
              ))}
            </div>
            <div className={styles.nav}>
              <Button variant="ghost" onClick={() => setStep(1)}>{t('onboarding.back')}</Button>
              <Button onClick={() => setStep(3)}>{t('onboarding.next')}</Button>
            </div>
          </section>
        )}

        {step === 3 && (
          <section className={styles.step}>
            <h3 className={styles.stepTitle}>{t('onboarding.step3Title')}</h3>
            <p className={styles.text}>{t('onboarding.step3Text')}</p>
            <div className={styles.bigButtons}>
              <Button variant="accent" className={styles.big} onClick={() => finish('/offers/new')}>
                🎁 {t('home.give')}
              </Button>
              <Button variant="secondary" className={styles.big} onClick={() => finish(browseTo)}>
                🔍 {t('subs.browseOffers')}
              </Button>
            </div>
            <div className={styles.nav}>
              <Button variant="ghost" onClick={() => setStep(2)}>{t('onboarding.back')}</Button>
            </div>
          </section>
        )}
      </div>
    </main>
  );
}
