import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { TransactionFilters } from '../../types/wallet';

// Mock Sheet to render children directly (Sheet uses portal which doesn't work in test)
// Also mock GroupItem to render title as text (title prop is not rendered as text content by the ui-kit)
vi.mock('@telegram-tools/ui-kit', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@telegram-tools/ui-kit')>();
  return {
    ...actual,
    Sheet: ({ open, children }: { open: boolean; children: React.ReactNode }) =>
      open ? <div data-testid="sheet">{children}</div> : null,
    GroupItem: ({ title, onClick, after }: { title: string; onClick?: () => void; after?: React.ReactNode }) => (
      <button type="button" data-group-item="true" onClick={onClick}>
        <span>{title}</span>
        {after && <span>{after}</span>}
      </button>
    ),
  };
});

import { TransactionFilterSheet } from '../TransactionFilterSheet';

function renderSheet(
  overrides: Partial<{
    open: boolean;
    onClose: () => void;
    filters: TransactionFilters;
    onApply: (f: TransactionFilters) => void;
    onReset: () => void;
  }> = {},
) {
  const props = {
    open: true,
    onClose: vi.fn(),
    filters: {},
    onApply: vi.fn(),
    onReset: vi.fn(),
    ...overrides,
  };
  return { ...renderWithProviders(<TransactionFilterSheet {...props} />), ...props };
}

describe('TransactionFilterSheet', () => {
  it('renders all 4 type options when open', () => {
    renderSheet();
    expect(screen.getByText('Escrow deposit')).toBeInTheDocument();
    expect(screen.getByText('Payout')).toBeInTheDocument();
    expect(screen.getByText('Refund')).toBeInTheDocument();
    expect(screen.getByText('Commission')).toBeInTheDocument();
  });

  it('shows checkmark for selected type', () => {
    renderSheet({ filters: { type: 'payout' } });
    expect(screen.getByText('\u2713')).toBeInTheDocument();
  });

  it('toggles type on click — selects unselected type', async () => {
    const { user, onApply } = renderSheet();
    await user.click(screen.getByText('Payout'));
    expect(onApply).toHaveBeenCalledWith({ type: 'payout' });
  });

  it('toggles type on click — deselects selected type', async () => {
    const { user, onApply } = renderSheet({ filters: { type: 'payout' } });
    await user.click(screen.getByText('Payout'));
    expect(onApply).toHaveBeenCalledWith({ type: undefined });
  });

  it('calls onReset on Reset button', async () => {
    const { user, onReset } = renderSheet();
    await user.click(screen.getByText('Reset'));
    expect(onReset).toHaveBeenCalledTimes(1);
  });

  it('calls onClose on Apply button', async () => {
    const { user, onClose } = renderSheet();
    await user.click(screen.getByText('Apply'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
