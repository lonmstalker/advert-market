import { renderWithProviders, screen } from '@/test/test-utils';
import type { WalletSummary } from '../types/wallet';
import { BalanceCard } from './BalanceCard';

vi.mock('@tonconnect/ui-react', () => ({
  TonConnectButton: () => <div data-testid="ton-connect-button" />,
}));

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

function renderCard(props: Partial<Parameters<typeof BalanceCard>[0]> = {}) {
  return renderWithProviders(
    <BalanceCard summary={ownerSummary} isOwner={true} isConnectionRestored={true} {...props} />,
  );
}

describe('BalanceCard', () => {
  it('renders "Total earned" label for owner', () => {
    renderCard();
    expect(screen.getByText('Total earned')).toBeInTheDocument();
  });

  it('renders "Total spent" label for advertiser', () => {
    renderCard({ summary: advertiserSummary, isOwner: false });
    expect(screen.getByText('Total spent')).toBeInTheDocument();
  });

  it('renders formatted balance amount with tabular-nums class', () => {
    renderCard();
    const balanceEl = screen.getByText('15 TON');
    expect(balanceEl).toBeInTheDocument();
    expect(balanceEl).toHaveClass('am-tabnum');
  });

  it('renders fiat equivalent with tabular-nums class', () => {
    renderCard();
    const fiatEl = screen.getByText('~$82.50');
    expect(fiatEl).toBeInTheDocument();
    expect(fiatEl).toHaveClass('am-tabnum');
  });

  it('renders TonConnectButton when connection restored', () => {
    renderCard({ isConnectionRestored: true });
    expect(screen.getByTestId('ton-connect-button')).toBeInTheDocument();
  });

  it('renders Spinner when connection NOT restored', () => {
    renderCard({ isConnectionRestored: false });
    expect(screen.queryByTestId('ton-connect-button')).not.toBeInTheDocument();
  });

  it('uses success gradient for owner', () => {
    const { container } = renderCard({ isOwner: true });
    const gradient = container.querySelector('[data-testid="balance-gradient"]');
    expect(gradient).toHaveStyle({ background: 'var(--am-hero-gradient-success)' });
  });

  it('uses accent gradient for advertiser', () => {
    const { container } = renderCard({ isOwner: false, summary: advertiserSummary });
    const gradient = container.querySelector('[data-testid="balance-gradient"]');
    expect(gradient).toHaveStyle({ background: 'var(--am-hero-gradient-accent)' });
  });
});
