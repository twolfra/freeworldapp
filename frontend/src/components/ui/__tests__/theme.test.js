import { beforeEach, describe, expect, it } from 'vitest';
import { applyTheme, getTheme, setTheme } from '../../../theme';

describe('theme', () => {
  beforeEach(() => {
    localStorage.clear();
    delete document.documentElement.dataset.theme;
  });

  it('setTheme stamps data-theme on <html> and persists the choice', () => {
    setTheme('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(localStorage.getItem('fw_theme')).toBe('dark');

    setTheme('light');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(localStorage.getItem('fw_theme')).toBe('light');
  });

  it('getTheme returns null for unset or invalid stored values', () => {
    expect(getTheme()).toBeNull();
    localStorage.setItem('fw_theme', 'purple');
    expect(getTheme()).toBeNull();
  });

  it('applyTheme resolves to the system preference when no choice is stored', () => {
    applyTheme();
    // jsdom reports prefers-color-scheme: dark as false → light.
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(getTheme()).toBeNull();
  });
});
