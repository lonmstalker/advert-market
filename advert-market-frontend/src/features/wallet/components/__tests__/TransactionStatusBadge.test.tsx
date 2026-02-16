import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { TRANSACTION_STATUSES, type TransactionStatus } from '../../types/wallet';
import { TransactionStatusBadge } from '../TransactionStatusBadge';

describe('TransactionStatusBadge', () => {
  it.each(TRANSACTION_STATUSES)('renders label for status "%s"', (status) => {
    renderWithProviders(<TransactionStatusBadge status={status} />);
    const expectedLabels: Record<TransactionStatus, string> = {
      pending: 'Pending',
      confirmed: 'Confirmed',
      failed: 'Failed',
    };
    expect(screen.getByText(expectedLabels[status])).toBeInTheDocument();
  });

  it('applies correct CSS class per status', () => {
    const expectedClass: Record<TransactionStatus, string> = {
      pending: 'bg-soft-warning',
      confirmed: 'bg-soft-success',
      failed: 'bg-soft-destructive',
    };

    for (const status of TRANSACTION_STATUSES) {
      const { unmount } = renderWithProviders(<TransactionStatusBadge status={status} />);
      const badge = screen.getByTestId('transaction-status-badge');
      expect(badge.className).toContain(expectedClass[status]);
      unmount();
    }
  });
});
