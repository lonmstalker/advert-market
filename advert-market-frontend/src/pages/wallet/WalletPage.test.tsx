import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router';
import { mockWalletSummaryEmpty, mockWalletSummaryOwner } from '@/test/mocks/data';
import { server } from '@/test/mocks/server';
import { renderWithProviders, screen } from '@/test/test-utils';
import WalletPage from './WalletPage';

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
  it('shows spinner during load', () => {
    server.use(
      http.get(`${API_BASE}/wallet/summary`, async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000));
        return HttpResponse.json(mockWalletSummaryOwner);
      }),
    );
    renderPage();
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

  it('renders Finance title', async () => {
    renderPage();
    expect(await screen.findByText('Finance')).toBeInTheDocument();
  });

  it('renders SummaryHero with earned amount', async () => {
    renderPage();
    expect(await screen.findByText('Total earned')).toBeInTheDocument();
    expect(screen.getByText('15 TON')).toBeInTheDocument();
  });

  it('renders SummaryStats section with 3 GroupItems', async () => {
    renderPage();
    await screen.findByText('Total earned');
    const items = document.querySelectorAll('[data-group-item]');
    expect(items.length).toBe(3);
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
});
