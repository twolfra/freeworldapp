// Single source of truth for the `fw_onboarded` localStorage flag —
// set once the post-registration mini-onboarding was completed or skipped.
// Kept in its own module (not Onboarding.jsx) so Login.jsx can import it
// without pulling in the page component.
const KEY = 'fw_onboarded';

export const hasOnboarded = () => localStorage.getItem(KEY) === '1';

export const markOnboarded = () => localStorage.setItem(KEY, '1');
