import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import EmptyState from '../EmptyState';

describe('EmptyState', () => {
  it('renders icon, title, text, and the CTA slot', () => {
    render(
      <EmptyState
        icon="📭"
        title="Nothing here yet"
        text="Be the first to give something away."
        action={<a href="/offers/new">Give something</a>}
      />,
    );
    expect(screen.getByText('📭')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Nothing here yet' })).toBeInTheDocument();
    expect(screen.getByText('Be the first to give something away.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Give something' })).toBeInTheDocument();
  });
});
