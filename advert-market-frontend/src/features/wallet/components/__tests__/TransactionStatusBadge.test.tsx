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

  it('applies correct background color per status', () => {
    const expectedBg: Record<TransactionStatus, string> = {
      pending: 'var(--am-soft-warning-bg)',
      confirmed: 'var(--am-soft-success-bg)',
      failed: 'var(--am-soft-destructive-bg)',
    };

    for (const status of TRANSACTION_STATUSES) {
      const { unmount } = renderWithProviders(<TransactionStatusBadge status={status} />);
      const outerSpan = document.querySelector('span[style*="background-color"]');
      expect(outerSpan).toBeTruthy();
      expect((outerSpan as HTMLElement).style.backgroundColor).toBe(expectedBg[status]);
      unmount();
    }
  });
});
