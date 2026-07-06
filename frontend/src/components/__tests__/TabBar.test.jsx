import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockUseAuth = vi.fn();
vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

const mockUseUnreadCount = vi.fn();
vi.mock('../../hooks/useUnreadCount', () => ({
  default: () => mockUseUnreadCount(),
}));

import TabBar from '../TabBar';

function renderTabBar() {
  return render(
    <MemoryRouter>
      <TabBar />
    </MemoryRouter>,
  );
}

describe('TabBar', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue({ user: { id: 'u1', username: 'tim', token: 'tok' } });
    mockUseUnreadCount.mockReturnValue({ unread: 0, notifUnread: 0 });
  });

  it('renders five tabs when signed in, with Profile linking to the own profile', () => {
    renderTabBar();

    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(5);

    expect(screen.getByRole('link', { name: /Discover/ })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: /Search/ })).toHaveAttribute('href', '/search');
    expect(screen.getByRole('link', { name: /Give/ })).toHaveAttribute('href', '/offers/new');
    expect(screen.getByRole('link', { name: /Messages/ })).toHaveAttribute('href', '/messages');
    expect(screen.getByRole('link', { name: /Profile/ })).toHaveAttribute('href', '/users/u1');
  });

  it('points the Profile tab to /login when signed out', () => {
    mockUseAuth.mockReturnValue({ user: null });
    renderTabBar();

    expect(screen.getByRole('link', { name: /Profile/ })).toHaveAttribute('href', '/login');
  });

  it('shows the unread badge on the Messages tab when unread > 0', () => {
    mockUseUnreadCount.mockReturnValue({ unread: 3, notifUnread: 0 });
    renderTabBar();

    expect(screen.getByRole('link', { name: /Messages/ })).toHaveTextContent('3');
  });

  it('shows no badge when there are no unread messages', () => {
    renderTabBar();

    expect(screen.getByRole('link', { name: /Messages/ })).not.toHaveTextContent(/\d/);
  });
});
