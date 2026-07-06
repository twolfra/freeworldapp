import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostalCodeInput from '../PostalCodeInput';
import { geo } from '../../api/client';

vi.mock('../../api/client', () => ({
  geo: { postal: vi.fn() },
}));

const LEIPZIG = { plz: '04315', city: 'Leipzig', lat: 51.34, lon: 12.39 };
const LEIPZIG2 = { plz: '04316', city: 'Leipzig', lat: 51.35, lon: 12.44 };

// Harness mirroring how the pages use the component (controlled value + selection).
function Harness({ onSelect }) {
  const [text, setText] = useState('');
  return (
    <PostalCodeInput
      value={text}
      onChange={setText}
      onSelect={(item) => {
        setText(`${item.plz} ${item.city}`);
        onSelect(item);
      }}
      label="Postal code / city"
      placeholder="e.g. 04315"
    />
  );
}

describe('PostalCodeInput', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches suggestions after typing and fills the input on selection', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    geo.postal.mockResolvedValue([LEIPZIG, LEIPZIG2]);
    render(<Harness onSelect={onSelect} />);

    const input = screen.getByRole('combobox', { name: 'Postal code / city' });
    await user.type(input, '043');

    // Debounced lookup with the typed prefix.
    await waitFor(() => expect(geo.postal).toHaveBeenCalledWith('043'), { timeout: 2000 });
    const option = await screen.findByRole('option', { name: '04315 Leipzig' });
    await user.click(option);

    expect(onSelect).toHaveBeenCalledWith(LEIPZIG);
    expect(input).toHaveValue('04315 Leipzig');
    // Dropdown closes after selection.
    expect(screen.queryByRole('option')).not.toBeInTheDocument();
  });

  it('supports keyboard navigation: arrow down + enter selects an option', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    geo.postal.mockResolvedValue([LEIPZIG, LEIPZIG2]);
    render(<Harness onSelect={onSelect} />);

    const input = screen.getByRole('combobox', { name: 'Postal code / city' });
    await user.type(input, '043');
    await screen.findByRole('option', { name: '04315 Leipzig' });

    await user.keyboard('{ArrowDown}{ArrowDown}{Enter}');

    expect(onSelect).toHaveBeenCalledWith(LEIPZIG2);
    expect(input).toHaveValue('04316 Leipzig');
  });

  it('does not query for fewer than 2 characters', async () => {
    const user = userEvent.setup();
    geo.postal.mockResolvedValue([LEIPZIG]);
    render(<Harness onSelect={vi.fn()} />);

    await user.type(screen.getByRole('combobox'), '0');
    // Wait past the debounce window, then confirm no lookup happened.
    await new Promise((resolve) => setTimeout(resolve, 400));
    expect(geo.postal).not.toHaveBeenCalled();
  });
});
