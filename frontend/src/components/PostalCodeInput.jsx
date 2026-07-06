import { useEffect, useId, useRef, useState } from 'react';
import { geo } from '../api/client';
import { t } from '../i18n';
import styles from './PostalCodeInput.module.css';

/**
 * Controlled postal-code / city input with a debounced autocomplete dropdown
 * backed by GET /api/geo/postal (local PLZ table — no external calls).
 *
 * Props:
 * - value            controlled input text
 * - onChange(text)   called on typing — parents should invalidate any previous selection
 * - onSelect(item)   called with { plz, city, lat, lon } when a suggestion is picked
 * - label            optional visible label (also used as aria-label fallback)
 * - placeholder, required, disabled, id
 * - inputClassName   extra class for the <input> so callers can match their styling
 */
export default function PostalCodeInput({
  value,
  onChange,
  onSelect,
  label,
  placeholder,
  required,
  disabled,
  id,
  inputClassName = '',
}) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const listId = `${inputId}-listbox`;
  const [options, setOptions] = useState([]);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [noResults, setNoResults] = useState(false);
  // Only fetch suggestions for changes typed by the user — not when the
  // parent sets the value programmatically (e.g. after a selection).
  const typedRef = useRef(false);

  useEffect(() => {
    if (!typedRef.current) return;
    typedRef.current = false;
    const timer = setTimeout(() => {
      const q = value.trim();
      if (q.length < 2) {
        setOptions([]);
        setOpen(false);
        setNoResults(false);
        return;
      }
      geo.postal(q)
        .then((res) => {
          setOptions(res);
          setActive(-1);
          setNoResults(res.length === 0);
          setOpen(true);
        })
        .catch(() => {
          setOptions([]);
          setOpen(false);
        });
    }, 250);
    return () => clearTimeout(timer);
  }, [value]);

  function close() {
    setOpen(false);
    setActive(-1);
  }

  function select(item) {
    close();
    setOptions([]);
    onSelect?.(item);
  }

  function handleInput(e) {
    typedRef.current = true;
    onChange?.(e.target.value);
  }

  function handleKeyDown(e) {
    if (e.key === 'Escape') {
      if (open) { e.preventDefault(); close(); }
      return;
    }
    if (!open || options.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((a) => (a + 1) % options.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((a) => (a <= 0 ? options.length - 1 : a - 1));
    } else if (e.key === 'Enter') {
      if (active >= 0) {
        e.preventDefault();
        select(options[active]);
      }
    }
  }

  return (
    <div className={styles.wrap}>
      {label && <label className={styles.label} htmlFor={inputId}>{label}</label>}
      <input
        id={inputId}
        className={[styles.input, inputClassName].filter(Boolean).join(' ')}
        type="text"
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={open && active >= 0 ? `${inputId}-opt-${active}` : undefined}
        aria-label={label ? undefined : t('form.postal')}
        autoComplete="off"
        value={value}
        onChange={handleInput}
        onKeyDown={handleKeyDown}
        onBlur={close}
        placeholder={placeholder ?? t('form.postalPlaceholder')}
        required={required}
        disabled={disabled}
        maxLength={80}
      />
      {open && (
        <ul className={styles.list} role="listbox" id={listId}>
          {noResults
            ? <li className={styles.noResults} aria-disabled="true">{t('postal.noResults')}</li>
            : options.map((o, i) => (
                <li
                  key={`${o.plz}-${o.city}`}
                  id={`${inputId}-opt-${i}`}
                  role="option"
                  aria-selected={i === active}
                  className={i === active ? `${styles.option} ${styles.optionActive}` : styles.option}
                  // preventDefault keeps the input focused so onBlur doesn't
                  // close the list before the click lands.
                  onMouseDown={(e) => e.preventDefault()}
                  onMouseEnter={() => setActive(i)}
                  onClick={() => select(o)}
                >
                  <span className={styles.plz}>{o.plz}</span> {o.city}
                </li>
              ))}
        </ul>
      )}
    </div>
  );
}
