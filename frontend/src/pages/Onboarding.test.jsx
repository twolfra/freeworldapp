import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../auth/AuthContext';
import { ToastProvider } from '../components/ui';
import Onboarding from './Onboarding';
import { users as usersApi } from '../api/client';

vi.mock('../api/client', () => ({
  auth: { login: vi.fn(), logout: vi.fn() },
  users: { updateProfile: vi.fn() },
}));

const storedUser = { id: 'u1', username: 'tim', token: 'tok-1' };

function renderOnboarding() {
  return render(
    <AuthProvider>
      <ToastProvider>
        <MemoryRouter initialEntries={['/welcome']}>
          <Routes>
            <Route path="/welcome" element={<Onboarding />} />
            <Route path="/offers" element={<div>offers-page-probe</div>} />
            <Route path="/offers/new" element={<div>offer-form-probe</div>} />
            <Route path="/login" element={<div>login-page-probe</div>} />
          </Routes>
        </MemoryRouter>
      </ToastProvider>
    </AuthProvider>,
  );
}

describe('Onboarding page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('currentUser', JSON.stringify(storedUser));
  });

  it('walks through the 3-step flow and finishes via a get-started button', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    // Step 1 — location.
    expect(screen.getByText('Step 1 of 3')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Where are you?' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    // Nothing entered → nothing saved.
    expect(usersApi.updateProfile).not.toHaveBeenCalled();

    // Step 2 — category chips.
    expect(screen.getByText('Step 2 of 3')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'What interests you?' })).toBeInTheDocument();
    const chip = screen.getByRole('button', { name: 'Furniture' });
    await user.click(chip);
    expect(chip).toHaveAttribute('aria-pressed', 'true');
    await user.click(screen.getByRole('button', { name: 'Next' }));

    // Step 3 — get started.
    expect(screen.getByText('Step 3 of 3')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Get started' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Give something away/ })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Browse offers/ }));

    expect(localStorage.getItem('fw_onboarded')).toBe('1');
    expect(await screen.findByText('offers-page-probe')).toBeInTheDocument();
  });

  it('skip sets the onboarded flag and navigates to /offers', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await user.click(screen.getByRole('button', { name: 'Skip for now' }));

    expect(localStorage.getItem('fw_onboarded')).toBe('1');
    expect(await screen.findByText('offers-page-probe')).toBeInTheDocument();
  });

  it('saves city and postal code on step 1 and advances', async () => {
    const user = userEvent.setup();
    usersApi.updateProfile.mockResolvedValue({ city: 'Leipzig', postalCode: '04315' });
    renderOnboarding();

    await user.type(screen.getByLabelText('City'), 'Leipzig');
    await user.type(screen.getByLabelText('Postal code'), '04315');
    await user.click(screen.getByRole('button', { name: 'Next' }));

    expect(usersApi.updateProfile).toHaveBeenCalledWith('u1', { city: 'Leipzig', postalCode: '04315' });
    expect(await screen.findByText('Step 2 of 3')).toBeInTheDocument();
    // The stored user picked up the profile fields (token untouched).
    expect(JSON.parse(localStorage.getItem('currentUser'))).toMatchObject({
      city: 'Leipzig',
      postalCode: '04315',
      token: 'tok-1',
    });
  });

  it('redirects signed-out visitors to /login', async () => {
    localStorage.removeItem('currentUser');
    renderOnboarding();

    expect(await screen.findByText('login-page-probe')).toBeInTheDocument();
  });
});
