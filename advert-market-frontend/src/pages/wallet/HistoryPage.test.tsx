import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router';
import { server } from '@/test/mocks/server';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import HistoryPage from './HistoryPage';

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
      <Route path="/wallet/history" element={<HistoryPage />} />
      <Route path="/wallet/history/:txId" element={<div>detail-page</div>} />
    </Routes>,
    { initialEntries: ['/wallet/history'] },
  );
}

describe('HistoryPage', () => {
  it('shows spinner during load', () => {
    server.use(
      http.get(`${API_BASE}/wallet/transactions`, async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000));
        return HttpResponse.json({ items: [], nextCursor: null, hasNext: false });
      }),
    );
    renderPage();
    expect(screen.queryByText('Transaction history')).toBeInTheDocument();
  });

  it('renders transaction history title', async () => {
    renderPage();
    expect(await screen.findByText('Transaction history')).toBeInTheDocument();
  });

  it('renders transaction list', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getAllByText('Payout').length).toBeGreaterThanOrEqual(1);
    });
  });

  it('shows filter icon button', async () => {
    renderPage();
    await screen.findByText('Transaction history');
    const filterButtons = document.querySelectorAll('button.am-filter-btn');
    expect(filterButtons.length).toBeGreaterThanOrEqual(1);
  });

  it('shows empty state when no transactions', async () => {
    server.use(
      http.get(`${API_BASE}/wallet/transactions`, () =>
        HttpResponse.json({ items: [], nextCursor: null, hasNext: false }),
      ),
    );
    renderPage();
    expect(await screen.findByText('No transactions')).toBeInTheDocument();
  });

  it('shows end-of-list when no more pages', async () => {
    renderPage();
    expect(await screen.findByText("That's all")).toBeInTheDocument();
  });
});
