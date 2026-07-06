// Light/dark theme management.
//
// localStorage `fw_theme` = 'light' | 'dark' | unset (unset → follow the
// OS `prefers-color-scheme` preference). The resolved theme is stamped as
// `data-theme` on <html>; index.css switches semantic tokens on it.

const THEME_KEY = 'fw_theme';
const DARK_QUERY = '(prefers-color-scheme: dark)';

let mediaQuery = null;

function systemPrefersDark() {
  return typeof window.matchMedia === 'function' && window.matchMedia(DARK_QUERY).matches;
}

/** The user's stored choice: 'light' | 'dark' | null (null → auto/system). */
export function getTheme() {
  const stored = localStorage.getItem(THEME_KEY);
  return stored === 'light' || stored === 'dark' ? stored : null;
}

/** The theme actually in effect right now: always 'light' or 'dark'. */
export function resolvedTheme() {
  return getTheme() ?? (systemPrefersDark() ? 'dark' : 'light');
}

function stamp() {
  document.documentElement.dataset.theme = resolvedTheme();
}

function onSystemChange() {
  if (!getTheme()) stamp(); // only follow the OS while in auto mode
}

/** Call once at startup: stamps the theme and tracks OS changes in auto mode. */
export function applyTheme() {
  stamp();
  if (!mediaQuery && typeof window.matchMedia === 'function') {
    mediaQuery = window.matchMedia(DARK_QUERY);
    mediaQuery.addEventListener?.('change', onSystemChange);
  }
}

/** Persist a choice ('light' | 'dark'; anything else clears back to auto) and restamp. */
export function setTheme(theme) {
  if (theme === 'light' || theme === 'dark') {
    localStorage.setItem(THEME_KEY, theme);
  } else {
    localStorage.removeItem(THEME_KEY);
  }
  stamp();
}
