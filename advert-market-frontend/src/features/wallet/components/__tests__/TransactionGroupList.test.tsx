import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { Transaction } from '../../types/wallet';
import { TransactionGroupList } from '../TransactionGroupList';

function makeTx(id: string, createdAt: string, overrides: Partial<Transaction> = {}): Transaction {
  return {
    id,
    type: 'payout',
    status: 'confirmed',
    amountNano: '1000000000',
    direction: 'income',
    dealId: null,
    channelTitle: `Channel ${id}`,
    description: 'Tx description',
    createdAt,
    ...overrides,
  };
}

describe('TransactionGroupList', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-02-14T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders day group labels', () => {
    const txs = [makeTx('a', '2026-02-14T10:00:00Z'), makeTx('b', '2026-02-13T09:00:00Z')];
    renderWithProviders(<TransactionGroupList transactions={txs} onItemClick={vi.fn()} />);
    expect(screen.getByText('Today')).toBeInTheDocument();
    expect(screen.getByText('Yesterday')).toBeInTheDocument();
  });

  it('renders items within groups', () => {
    const txs = [makeTx('a', '2026-02-14T10:00:00Z'), makeTx('b', '2026-02-14T08:00:00Z')];
    renderWithProviders(<TransactionGroupList transactions={txs} onItemClick={vi.fn()} />);
    expect(screen.getByText('Channel a')).toBeInTheDocument();
    expect(screen.getByText('Channel b')).toBeInTheDocument();
  });

  it('calls onItemClick with tx id', async () => {
    // userEvent.click() doesn't work with fake timers — switch to real before clicking
    vi.useRealTimers();

    const onItemClick = vi.fn();
    const now = new Date();
    const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}T10:00:00Z`;
    const txs = [makeTx('tx-42', todayStr)];
    const { user } = renderWithProviders(<TransactionGroupList transactions={txs} onItemClick={onItemClick} />);
    const channelText = screen.getByText('Channel tx-42');
    await user.click(channelText);
    expect(onItemClick).toHaveBeenCalledWith('tx-42');
  });

  it('renders empty for empty array', () => {
    const { container } = renderWithProviders(<TransactionGroupList transactions={[]} onItemClick={vi.fn()} />);
    expect(screen.queryByText('Today')).not.toBeInTheDocument();
    expect(container.textContent).toBe('');
  });
});
