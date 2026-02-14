import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { WalletSummary } from '../../types/wallet';
import { SummaryStats } from '../SummaryStats';

const ownerSummary: WalletSummary = {
  earnedTotalNano: '15000000000',
  inEscrowNano: '5000000000',
  spentTotalNano: '0',
  activeEscrowNano: '0',
  activeDealsCount: 2,
  completedDealsCount: 5,
};

const advertiserSummary: WalletSummary = {
  earnedTotalNano: '0',
  inEscrowNano: '0',
  spentTotalNano: '25000000000',
  activeEscrowNano: '8000000000',
  activeDealsCount: 3,
  completedDealsCount: 4,
};

describe('SummaryStats', () => {
  it('shows inEscrowNano for owner', () => {
    renderWithProviders(<SummaryStats summary={ownerSummary} />);
    expect(screen.getByText('5 TON')).toBeInTheDocument();
  });

  it('shows activeEscrowNano for advertiser', () => {
    renderWithProviders(<SummaryStats summary={advertiserSummary} />);
    expect(screen.getByText('8 TON')).toBeInTheDocument();
  });

  it('shows completed and active deals counts', () => {
    renderWithProviders(<SummaryStats summary={ownerSummary} />);
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('renders Group with 3 GroupItems', () => {
    const { container } = renderWithProviders(<SummaryStats summary={ownerSummary} />);
    const items = container.querySelectorAll('[data-group-item]');
    expect(items).toHaveLength(3);
  });
});
