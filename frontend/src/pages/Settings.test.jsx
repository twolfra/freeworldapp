import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../auth/AuthContext';
import { ToastProvider } from '../components/ui';
import Settings from './Settings';
import { users as usersApi, auth as authApi } from '../api/client';

vi.mock('../api/client', () => ({
  auth: { logout: vi.fn().mockResolvedValue(null), changePassword: vi.fn() },
  users: { update: vi.fn(), updateProfile: vi.fn(), remove: vi.fn() },
  images: { upload: vi.fn() },
  notifications: { updatePreferences: vi.fn() },
}));

const STORED_USER = {
  id: 'u1',
  username: 'tim',
  email: 'tim@example.com',
  token: 'tok-1',
  displayName: null,
  bio: null,
  avatarUrl: null,
  postalCode: null,
  city: null,
  notifyOnMessage: true,
};

function renderSettings({ signedIn = true } = {}) {
  if (signedIn) localStorage.setItem('currentUser', JSON.stringify(STORED_USER));
  return render(
    <AuthProvider>
      <ToastProvider>
        <MemoryRouter initialEntries={['/settings']}>
          <Routes>
            <Route path="/settings" element={<Settings />} />
            <Route path="/" element={<div>home-page-probe</div>} />
          </Routes>
        </MemoryRouter>
      </ToastProvider>
    </AuthProvider>,
  );
}

describe('Settings page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('asks signed-out visitors to sign in', () => {
    renderSettings({ signedIn: false });

    expect(screen.getByRole('link', { name: 'sign in' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Settings' })).not.toBeInTheDocument();
  });

  it('renders all four section tabs and the profile form when signed in', () => {
    renderSettings();

    expect(screen.getByRole('heading', { name: 'Settings' })).toBeInTheDocument();
    for (const tab of ['Profile', 'Account', 'Notifications', 'Language']) {
      expect(screen.getByRole('button', { name: tab })).toBeInTheDocument();
    }
    // Profile tab is active by default.
    expect(screen.getByLabelText('Display name')).toBeInTheDocument();
    expect(screen.getByLabelText('City')).toBeInTheDocument();
  });

  it('saves only changed profile fields via users.updateProfile and updates the stored user', async () => {
    const user = userEvent.setup();
    usersApi.updateProfile.mockResolvedValue({
      ...STORED_USER,
      city: 'Leipzig',
      bio: 'Hello there',
    });
    renderSettings();

    await user.type(screen.getByLabelText('City'), 'Leipzig');
    await user.type(screen.getByLabelText('About you'), 'Hello there');
    await user.click(screen.getByRole('button', { name: 'Save profile' }));

    expect(await screen.findByText('Profile saved.')).toBeInTheDocument();
    expect(usersApi.updateProfile).toHaveBeenCalledTimes(1);
    expect(usersApi.updateProfile).toHaveBeenCalledWith('u1', {
      city: 'Leipzig',
      bio: 'Hello there',
    });
    expect(JSON.parse(localStorage.getItem('currentUser'))).toMatchObject({
      city: 'Leipzig',
      bio: 'Hello there',
      token: 'tok-1', // token must survive profile updates
    });
  });

  it('only deletes the account after the confirm dialog is accepted', async () => {
    const user = userEvent.setup();
    usersApi.remove.mockResolvedValue(null);
    renderSettings();

    await user.click(screen.getByRole('button', { name: 'Account' }));
    await user.click(screen.getByRole('button', { name: 'Delete account' }));

    // Modal open, nothing deleted yet.
    expect(usersApi.remove).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Delete forever' }));

    expect(usersApi.remove).toHaveBeenCalledWith('u1');
    // The session is gone server-side, so no logout call is made…
    expect(authApi.logout).not.toHaveBeenCalled();
    // …but the user is signed out locally and navigated home.
    expect(await screen.findByText('home-page-probe')).toBeInTheDocument();
    expect(localStorage.getItem('currentUser')).toBeNull();
  });
});
