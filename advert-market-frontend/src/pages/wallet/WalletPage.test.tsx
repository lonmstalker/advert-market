import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router';
import { mockWalletSummaryEmpty, mockWalletSummaryOwner } from '@/test/mocks/data';
import { server } from '@/test/mocks/server';
import { renderWithProviders, screen } from '@/test/test-utils';
import WalletPage from './WalletPage';

vi.mock('@tonconnect/ui-react', () => ({
  TonConnectButton: () => <div data-testid="ton-connect-button" />,
  useIsConnectionRestored: () => true,
}));

vi.mock('@/shared/ui', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/shared/ui')>();
  return { ...actual, BackButtonHandler: () => null };
});

vi.mock('@/shared/hooks/use-haptic', () => ({
  useHaptic: () => ({
    impactOccurred: vi.fn(),
    notificationOccurred: vi.fn(),
    selectionChanged: vi.fn(),
  }),
}));

const API_BASE = '/api/v1';

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/wallet" element={<WalletPage />} />
      <Route path="/wallet/history" element={<div>history-page</div>} />
      <Route path="/wallet/history/:txId" element={<div>detail-page</div>} />
      <Route path="/catalog" element={<div>catalog-page</div>} />
    </Routes>,
    { initialEntries: ['/wallet'] },
  );
}

describe('WalletPage', () => {
  it('shows skeleton during load', () => {
    server.use(
      http.get(`${API_BASE}/wallet/summary`, async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000));
        return HttpResponse.json(mockWalletSummaryOwner);
      }),
    );
    renderPage();
    // No Finance title during loading — skeleton shown instead
    expect(screen.queryByText('Finance')).not.toBeInTheDocument();
  });

  it('shows empty state when no data', async () => {
    server.use(
      http.get(`${API_BASE}/wallet/summary`, () => HttpResponse.json(mockWalletSummaryEmpty)),
      http.get(`${API_BASE}/wallet/transactions`, () =>
        HttpResponse.json({ items: [], nextCursor: null, hasNext: false }),
      ),
    );
    renderPage();
    expect(await screen.findByText('No transactions yet')).toBeInTheDocument();
  });

  it('renders finance header chip', async () => {
    renderPage();
    await screen.findByText('Total earned');
    expect(screen.getByText('Finance')).toBeInTheDocument();
  });

  it('renders BalanceCard with earned amount', async () => {
    renderPage();
    expect(await screen.findByText('Total earned')).toBeInTheDocument();
    expect(screen.getByText('15 TON')).toBeInTheDocument();
  });

  it('renders TonConnectButton inside BalanceCard', async () => {
    renderPage();
    await screen.findByText('Total earned');
    expect(screen.getByTestId('ton-connect-button')).toBeInTheDocument();
  });

  it('renders MetricRow with 2 cells', async () => {
    renderPage();
    await screen.findByText('Total earned');
    expect(screen.getByText('In escrow')).toBeInTheDocument();
    expect(screen.getByText('Completed deals')).toBeInTheDocument();
    expect(screen.getByText('5 TON')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('shows "View all" link to /wallet/history', async () => {
    renderPage();
    expect(await screen.findByText('View all')).toBeInTheDocument();
  });

  it('navigates to detail on transaction click', async () => {
    const { user } = renderPage();
    await screen.findByText('View all');
    const txItems = screen.getAllByText('Payout');
    await user.click(txItems[0]);
    expect(await screen.findByText('detail-page')).toBeInTheDocument();
  });

  it('renders wallet quick actions', async () => {
    renderPage();
    await screen.findByText('Total earned');
    expect(screen.getByRole('button', { name: 'Transfer' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Top up' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Withdraw' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Exchange' })).toBeInTheDocument();
  });
});
