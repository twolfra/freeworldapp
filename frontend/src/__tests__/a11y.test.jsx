import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { AuthProvider } from '../auth/AuthContext';
import Home from '../pages/Home';
import Login from '../pages/Login';
import OfferList from '../pages/OfferList';
import NotFound from '../pages/NotFound';
import { offers as offersApi, requests as requestsApi, search as searchApi } from '../api/client';

expect.extend(matchers);

vi.mock('../api/client', () => ({
  auth: { login: vi.fn(), logout: vi.fn() },
  geo: { postal: vi.fn().mockResolvedValue([]) },
  search: { run: vi.fn() },
  offers: { list: vi.fn() },
  requests: { list: vi.fn() },
}));

// Leaflet needs a real browser; the map view is not part of these checks.
vi.mock('../components/PostsMap', () => ({
  default: () => <div data-testid="posts-map" />,
}));

const items = [
  {
    id: '1', title: 'Free sofa', description: 'Comfy three-seater couch',
    region: 'Leipzig', category: 'Furniture', quantity: 1, imageUrl: null,
    createdAt: '2026-01-02T10:00:00Z',
  },
  {
    id: '2', title: 'Garden apples', description: 'Fresh from the tree',
    region: 'Berlin', category: 'Food & Drink', quantity: 5, imageUrl: null,
    createdAt: '2026-01-01T10:00:00Z',
  },
];

// The color-contrast rule needs a real rendering engine (layout + canvas)
// and cannot run under jsdom — contrast is verified by computation against
// the index.css tokens instead (see the token comments there). Everything
// else runs at the WCAG 2.1 A/AA level, matching the AP 4.2 acceptance
// criteria (axe without critical violations).
const AXE_OPTIONS = {
  runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] },
  rules: { 'color-contrast': { enabled: false } },
};

function renderPage(ui, entry = '/') {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[entry]}>{ui}</MemoryRouter>
    </AuthProvider>,
  );
}

describe('axe accessibility checks (AP 4.2)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('Home has no violations', async () => {
    offersApi.list.mockResolvedValue(items);
    requestsApi.list.mockResolvedValue([]);
    const { container, findByText } = renderPage(<Home />);
    await findByText('Free sofa');
    expect(await axe(container, AXE_OPTIONS)).toHaveNoViolations();
  });

  it('Login has no violations', async () => {
    const { container, findByRole } = renderPage(<Login />, '/login');
    await findByRole('heading', { name: 'Welcome back' });
    expect(await axe(container, AXE_OPTIONS)).toHaveNoViolations();
  });

  it('OfferList has no violations', async () => {
    searchApi.run.mockResolvedValue({ items, total: 2, page: 0, size: 12 });
    const { container, findByText } = renderPage(<OfferList />, '/offers');
    await findByText('Free sofa');
    expect(await axe(container, AXE_OPTIONS)).toHaveNoViolations();
  });

  it('NotFound has no violations', async () => {
    const { container, findByText } = renderPage(<NotFound />, '/does-not-exist');
    await findByText('Page not found');
    expect(await axe(container, AXE_OPTIONS)).toHaveNoViolations();
  });
});
