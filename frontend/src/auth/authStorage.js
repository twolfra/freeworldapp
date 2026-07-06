// Single source of truth for the `currentUser` localStorage key.
// Only AuthContext.jsx and api/client.js may import from here —
// no other module should touch localStorage for auth data.
const KEY = 'currentUser';

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(KEY) || 'null');
  } catch {
    return null;
  }
}

export function setStoredUser(user) {
  localStorage.setItem(KEY, JSON.stringify(user));
}

export function clearStoredUser() {
  localStorage.removeItem(KEY);
}
