import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../auth/AuthContext';
import OfferList from './OfferList';
import { search as searchApi } from '../api/client';

vi.mock('../api/client', () => ({
  auth: { login: vi.fn(), logout: vi.fn() },
  geo: { postal: vi.fn().mockResolvedValue([]) },
  search: { run: vi.fn() },
}));

// Leaflet needs a real browser; a stub is enough to prove the toggle renders it.
vi.mock('../components/PostsMap', () => ({
  default: ({ items }) => <div data-testid="posts-map">map with {items.length} items</div>,
}));

const mockOffers = [
  {
    id: '1', title: 'Free sofa', description: 'Comfy three-seater couch',
    region: 'Leipzig', category: 'Furniture', quantity: 1, imageUrl: null,
  },
  {
    id: '2', title: 'Garden apples', description: 'Fresh from the tree',
    region: 'Berlin', category: 'Food & Drink', quantity: 5, imageUrl: null,
  },
];

function mockResults(items, total = items.length) {
  searchApi.run.mockResolvedValue({ items, total, page: 0, size: 12 });
}

function lastSearchParams() {
  return searchApi.run.mock.calls[searchApi.run.mock.calls.length - 1][0];
}

function renderOfferList(initialEntry = '/offers') {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/offers" element={<OfferList />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
}

describe('OfferList page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders a card per offer with title, region and count from the search response', async () => {
    mockResults(mockOffers);
    renderOfferList();

    expect(await screen.findByText('Free sofa')).toBeInTheDocument();
    expect(screen.getByText('Garden apples')).toBeInTheDocument();
    expect(screen.getByText('Leipzig')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Offers/ })).toBeInTheDocument();
    expect(screen.getByText('2 available')).toBeInTheDocument();
    expect(searchApi.run).toHaveBeenCalledTimes(1);
    expect(lastSearchParams()).toMatchObject({ type: 'offers', page: 0, size: 12, sort: 'newest' });
  });

  it('shows the nothing-here EmptyState with a post CTA when there are no offers at all', async () => {
    mockResults([]);
    renderOfferList();

    expect(await screen.findByText('Nothing here yet')).toBeInTheDocument();
    expect(screen.getByText('Be the first to give something away to your community.')).toBeInTheDocument();
    const cta = screen.getByRole('link', { name: 'Give something away' });
    expect(cta).toHaveAttribute('href', '/offers/new');
    // The no-match variant must not show for a truly empty list.
    expect(screen.queryByText('No offers match your search.')).not.toBeInTheDocument();
  });

  it('shows the no-match EmptyState and clears filters via its CTA', async () => {
    const user = userEvent.setup();
    mockResults(mockOffers);
    renderOfferList();

    await screen.findByText('Free sofa');

    mockResults([], 0);
    await user.type(screen.getByPlaceholderText('Search by title, category…'), 'zzz');

    expect(await screen.findByText('No offers match your search.')).toBeInTheDocument();
    expect(screen.queryByText('Free sofa')).not.toBeInTheDocument();

    mockResults(mockOffers);
    await user.click(screen.getByRole('button', { name: 'Clear filters' }));

    expect(await screen.findByText('Free sofa')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Search by title, category…')).toHaveValue('');
  });

  it('sends the typed query to the search endpoint (debounced)', async () => {
    const user = userEvent.setup();
    mockResults(mockOffers);
    renderOfferList();

    await screen.findByText('Free sofa');
    mockResults([mockOffers[0]], 1);
    await user.type(screen.getByPlaceholderText('Search by title, category…'), 'sofa');

    await waitFor(() => expect(lastSearchParams()).toMatchObject({ q: 'sofa' }));
    expect(await screen.findByText('1 available')).toBeInTheDocument();
  });

  it('seeds the search from the ?q= URL param', async () => {
    mockResults([mockOffers[1]], 1);
    renderOfferList('/offers?q=apples');

    expect(await screen.findByText('Garden apples')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Search by title, category…')).toHaveValue('apples');
    expect(lastSearchParams()).toMatchObject({ q: 'apples' });
  });

  it('shows the API error message when loading fails', async () => {
    searchApi.run.mockRejectedValue(new Error('boom from the API'));
    renderOfferList();

    expect(await screen.findByText('boom from the API')).toBeInTheDocument();
  });

  it('shows the include-completed toggle and refetches when checked', async () => {
    const user = userEvent.setup();
    mockResults(mockOffers);
    renderOfferList();

    await screen.findByText('Free sofa');
    expect(lastSearchParams().includeCompleted).toBeUndefined();

    await user.click(screen.getByRole('checkbox', { name: 'Also show given away' }));

    await waitFor(() => expect(lastSearchParams()).toMatchObject({ includeCompleted: true }));
  });

  it('shows status badges and dims given-away cards', async () => {
    mockResults([
      { ...mockOffers[0], status: 'RESERVED' },
      { ...mockOffers[1], status: 'GIVEN' },
    ]);
    renderOfferList();

    expect(await screen.findByText('Reserved')).toBeInTheDocument();
    expect(screen.getByText('Given away')).toBeInTheDocument();
    // The GIVEN card link is dimmed via the cardCompleted class.
    const givenCard = screen.getByRole('link', { name: /Garden apples/ });
    expect(givenCard.className).toMatch(/cardCompleted/);
    const reservedCard = screen.getByRole('link', { name: /Free sofa/ });
    expect(reservedCard.className).not.toMatch(/cardCompleted/);
  });

  it('renders no status badge for active offers', async () => {
    mockResults([{ ...mockOffers[0], status: 'ACTIVE' }]);
    renderOfferList();

    await screen.findByText('Free sofa');
    expect(screen.queryByText('Reserved')).not.toBeInTheDocument();
    expect(screen.queryByText('Given away')).not.toBeInTheDocument();
  });

  it('shows a distance badge when the search response includes distanceKm', async () => {
    localStorage.setItem('fw_location', JSON.stringify({ plz: '04315', city: 'Leipzig', lat: 51.34, lon: 12.39 }));
    mockResults([{ ...mockOffers[0], lat: 51.35, lon: 12.4, postalCode: '04315', city: 'Leipzig', distanceKm: 1.2 }], 1);
    renderOfferList();

    expect(await screen.findByText('1.2 km')).toBeInTheDocument();
    // The stored location is sent along with the radius filter.
    expect(lastSearchParams()).toMatchObject({ lat: 51.34, lon: 12.39, radiusKm: 10 });
  });

  it('switches to the map view via the toggle and requests more items', async () => {
    const user = userEvent.setup();
    mockResults(mockOffers);
    renderOfferList();

    await screen.findByText('Free sofa');
    await user.click(screen.getByRole('button', { name: 'Map' }));

    expect(await screen.findByTestId('posts-map')).toBeInTheDocument();
    await waitFor(() => expect(lastSearchParams()).toMatchObject({ size: 200, page: 0 }));
  });
});
