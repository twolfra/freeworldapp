// Vitest setup — loaded before every test file (see vite.config.js `test.setupFiles`).
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Tests import describe/it/expect explicitly (globals are off),
// so react-testing-library's automatic cleanup must be registered manually.
afterEach(() => {
  cleanup();
  localStorage.clear();
});
