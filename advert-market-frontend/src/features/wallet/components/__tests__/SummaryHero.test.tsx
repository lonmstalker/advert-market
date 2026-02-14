import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { WalletSummary } from '../../types/wallet';
import { SummaryHero } from '../SummaryHero';

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

describe('SummaryHero', () => {
  it('shows "Total earned" for owner view (earnedTotalNano != 0)', () => {
    renderWithProviders(<SummaryHero summary={ownerSummary} />);
    expect(screen.getByText('Total earned')).toBeInTheDocument();
  });

  it('shows "Total spent" for advertiser view (earnedTotalNano == 0)', () => {
    renderWithProviders(<SummaryHero summary={advertiserSummary} />);
    expect(screen.getByText('Total spent')).toBeInTheDocument();
  });

  it('displays formatted TON amount for owner', () => {
    renderWithProviders(<SummaryHero summary={ownerSummary} />);
    expect(screen.getByText('15 TON')).toBeInTheDocument();
  });

  it('displays formatted TON amount for advertiser', () => {
    renderWithProviders(<SummaryHero summary={advertiserSummary} />);
    expect(screen.getByText('25 TON')).toBeInTheDocument();
  });

  it('displays fiat equivalent', () => {
    renderWithProviders(<SummaryHero summary={ownerSummary} />);
    // 15 TON * $5.5 = $82.50
    expect(screen.getByText('~$82.50')).toBeInTheDocument();
  });
});
