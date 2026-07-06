import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../auth/AuthContext';
import Login from './Login';
import { auth as authApi } from '../api/client';

vi.mock('../api/client', () => ({
  auth: { login: vi.fn(), logout: vi.fn() },
}));

function renderLogin() {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/offers" element={<div>offers-page-probe</div>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
}

describe('Login page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders username and password fields and a submit button', () => {
    renderLogin();

    expect(screen.getByRole('heading', { name: 'Welcome back' })).toBeInTheDocument();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign In' })).toBeInTheDocument();
  });

  it('logs in, stores the user, and navigates to /offers on success', async () => {
    const user = userEvent.setup();
    authApi.login.mockResolvedValue({ id: 'u1', username: 'tim', token: 'tok-1' });
    renderLogin();

    await user.type(screen.getByLabelText('Username'), 'tim');
    await user.type(screen.getByLabelText('Password'), 'secret');
    await user.click(screen.getByRole('button', { name: 'Sign In' }));

    expect(await screen.findByText('offers-page-probe')).toBeInTheDocument();
    expect(authApi.login).toHaveBeenCalledWith({ username: 'tim', password: 'secret' });
    expect(JSON.parse(localStorage.getItem('currentUser'))).toMatchObject({
      username: 'tim',
      token: 'tok-1',
    });
  });

  it('shows the bad-credentials error when login fails', async () => {
    const user = userEvent.setup();
    authApi.login.mockRejectedValue(new Error('Invalid username or password'));
    renderLogin();

    await user.type(screen.getByLabelText('Username'), 'tim');
    await user.type(screen.getByLabelText('Password'), 'wrong');
    await user.click(screen.getByRole('button', { name: 'Sign In' }));

    expect(await screen.findByText('Invalid username or password.')).toBeInTheDocument();
    // Still on the login page — no navigation happened.
    expect(screen.queryByText('offers-page-probe')).not.toBeInTheDocument();
  });

  it('shows the unverified-email hint with a resend link when the account is not verified', async () => {
    const user = userEvent.setup();
    authApi.login.mockRejectedValue(new Error('Email not verified'));
    renderLogin();

    await user.type(screen.getByLabelText('Username'), 'tim');
    await user.type(screen.getByLabelText('Password'), 'secret');
    await user.click(screen.getByRole('button', { name: 'Sign In' }));

    expect(await screen.findByText(/Email not verified\./)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Resend verification email' })).toBeInTheDocument();
  });
});
