import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { Transaction } from '../../types/wallet';
import { TransactionListItem } from '../TransactionListItem';

function makeTx(overrides: Partial<Transaction> = {}): Transaction {
  return {
    id: 'tx-1',
    type: 'payout',
    status: 'confirmed',
    amountNano: '5000000000',
    direction: 'income',
    dealId: 'deal-1',
    channelTitle: 'Crypto News Daily',
    description: 'Payout for completed deal',
    createdAt: '2026-02-14T10:30:00Z',
    ...overrides,
  };
}

describe('TransactionListItem', () => {
  it('renders type label from i18n', () => {
    renderWithProviders(<TransactionListItem transaction={makeTx()} onClick={vi.fn()} />);
    expect(screen.getByText('Payout')).toBeInTheDocument();
  });

  it('renders channel title when available', () => {
    renderWithProviders(<TransactionListItem transaction={makeTx()} onClick={vi.fn()} />);
    expect(screen.getByText('Crypto News Daily')).toBeInTheDocument();
  });

  it('renders description as fallback when channelTitle is null', () => {
    const tx = makeTx({ channelTitle: null, description: 'Refund for cancelled deal' });
    renderWithProviders(<TransactionListItem transaction={tx} onClick={vi.fn()} />);
    expect(screen.getByText('Refund for cancelled deal')).toBeInTheDocument();
  });

  it('renders amount with + for income', () => {
    renderWithProviders(<TransactionListItem transaction={makeTx({ direction: 'income' })} onClick={vi.fn()} />);
    expect(screen.getByText(/\+5 TON/)).toBeInTheDocument();
  });

  it('renders amount with minus for expense', () => {
    renderWithProviders(<TransactionListItem transaction={makeTx({ direction: 'expense' })} onClick={vi.fn()} />);
    expect(screen.getByText(/\u22125 TON/)).toBeInTheDocument();
  });

  it('applies success color for income direction', () => {
    renderWithProviders(<TransactionListItem transaction={makeTx({ direction: 'income' })} onClick={vi.fn()} />);
    const amountEl = screen.getByText(/\+5 TON/);
    expect(amountEl).toHaveStyle({ color: 'var(--color-state-success)' });
  });

  it('applies destructive color for expense direction', () => {
    renderWithProviders(<TransactionListItem transaction={makeTx({ direction: 'expense' })} onClick={vi.fn()} />);
    const amountEl = screen.getByText(/\u22125 TON/);
    expect(amountEl).toHaveStyle({ color: 'var(--color-state-destructive)' });
  });

  it('calls onClick when clicked', async () => {
    const onClick = vi.fn();
    const { user } = renderWithProviders(<TransactionListItem transaction={makeTx()} onClick={onClick} />);
    await user.click(screen.getByText('Payout'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
