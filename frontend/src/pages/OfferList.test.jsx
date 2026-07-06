import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../auth/AuthContext';
import OfferList from './OfferList';
import { offers as offersApi } from '../api/client';

vi.mock('../api/client', () => ({
  auth: { login: vi.fn(), logout: vi.fn() },
  offers: { list: vi.fn() },
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

  it('renders a card per offer with title, region and count', async () => {
    offersApi.list.mockResolvedValue(mockOffers);
    renderOfferList();

    expect(await screen.findByText('Free sofa')).toBeInTheDocument();
    expect(screen.getByText('Garden apples')).toBeInTheDocument();
    // "Leipzig" appears both in the card meta and as a region-filter option.
    expect(screen.getAllByText('Leipzig').length).toBeGreaterThanOrEqual(2);
    expect(screen.getByRole('option', { name: 'Leipzig' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Offers/ })).toBeInTheDocument();
    expect(screen.getByText('2 available')).toBeInTheDocument();
    expect(offersApi.list).toHaveBeenCalledTimes(1);
  });

  it('shows the translated empty state when there are no offers', async () => {
    offersApi.list.mockResolvedValue([]);
    renderOfferList();

    expect(await screen.findByText('No offers match your search.')).toBeInTheDocument();
  });

  it('filters the cards as the user types into the search box', async () => {
    const user = userEvent.setup();
    offersApi.list.mockResolvedValue(mockOffers);
    renderOfferList();

    await screen.findByText('Free sofa');
    await user.type(screen.getByPlaceholderText('Search by title, category…'), 'sofa');

    expect(screen.getByText('Free sofa')).toBeInTheDocument();
    expect(screen.queryByText('Garden apples')).not.toBeInTheDocument();
    expect(screen.getByText('1 available')).toBeInTheDocument();
  });

  it('seeds the search from the ?q= URL param', async () => {
    offersApi.list.mockResolvedValue(mockOffers);
    renderOfferList('/offers?q=apples');

    expect(await screen.findByText('Garden apples')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Search by title, category…')).toHaveValue('apples');
    expect(screen.queryByText('Free sofa')).not.toBeInTheDocument();
  });

  it('shows the API error message when loading fails', async () => {
    offersApi.list.mockRejectedValue(new Error('boom from the API'));
    renderOfferList();

    expect(await screen.findByText('boom from the API')).toBeInTheDocument();
  });

  it('shows the include-completed toggle and refetches when checked', async () => {
    const user = userEvent.setup();
    offersApi.list.mockResolvedValue(mockOffers);
    renderOfferList();

    await screen.findByText('Free sofa');
    expect(offersApi.list).toHaveBeenLastCalledWith(false);

    await user.click(screen.getByRole('checkbox', { name: 'Also show given away' }));

    expect(offersApi.list).toHaveBeenLastCalledWith(true);
    expect(offersApi.list).toHaveBeenCalledTimes(2);
  });

  it('shows status badges and dims given-away cards', async () => {
    offersApi.list.mockResolvedValue([
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
    offersApi.list.mockResolvedValue([{ ...mockOffers[0], status: 'ACTIVE' }]);
    renderOfferList();

    await screen.findByText('Free sofa');
    expect(screen.queryByText('Reserved')).not.toBeInTheDocument();
    expect(screen.queryByText('Given away')).not.toBeInTheDocument();
  });
});
