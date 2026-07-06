import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { ToastProvider, useToast } from '../Toast';

function Probe() {
  const toast = useToast();
  return (
    <>
      <button onClick={() => toast.success('Saved!')}>fire-success</button>
      <button onClick={() => toast.error('Boom')}>fire-error</button>
    </>
  );
}

function renderWithProvider() {
  return render(
    <ToastProvider>
      <Probe />
    </ToastProvider>,
  );
}

describe('Toast', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('shows a toast via useToast and auto-dismisses after ~4s', () => {
    renderWithProvider();
    fireEvent.click(screen.getByRole('button', { name: 'fire-success' }));
    expect(screen.getByText('Saved!')).toBeInTheDocument();

    act(() => vi.advanceTimersByTime(4100));
    expect(screen.queryByText('Saved!')).not.toBeInTheDocument();
  });

  it('stacks multiple toasts and removes one on its dismiss button', () => {
    renderWithProvider();
    fireEvent.click(screen.getByRole('button', { name: 'fire-success' }));
    fireEvent.click(screen.getByRole('button', { name: 'fire-error' }));
    expect(screen.getByText('Saved!')).toBeInTheDocument();
    expect(screen.getByText('Boom')).toBeInTheDocument();

    fireEvent.click(screen.getAllByRole('button', { name: 'Dismiss' })[0]);
    expect(screen.queryByText('Saved!')).not.toBeInTheDocument();
    expect(screen.getByText('Boom')).toBeInTheDocument();
  });
});
