import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router';
import { mockTransactionDetail, mockTransactionDetailMinimal } from '@/test/mocks/data';
import { server } from '@/test/mocks/server';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import TransactionDetailPage from './TransactionDetailPage';

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

const { mockCopyToClipboard } = vi.hoisted(() => ({
  mockCopyToClipboard: vi.fn().mockResolvedValue(true),
}));
vi.mock('@/shared/lib/clipboard', () => ({
  copyToClipboard: mockCopyToClipboard,
}));

const API_BASE = '/api/v1';

function renderPage(txId = 'tx-1') {
  return renderWithProviders(
    <Routes>
      <Route path="/wallet/history/:txId" element={<TransactionDetailPage />} />
      <Route path="/deals/:dealId" element={<div>deal-page</div>} />
    </Routes>,
    { initialEntries: [`/wallet/history/${txId}`] },
  );
}

describe('TransactionDetailPage', () => {
  it('shows loader during load', () => {
    server.use(
      http.get(`${API_BASE}/wallet/transactions/:txId`, async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000));
        return HttpResponse.json(mockTransactionDetail);
      }),
    );
    renderPage();
    expect(screen.queryByText('Escrow deposit')).not.toBeInTheDocument();
  });

  it('shows error for nonexistent tx', async () => {
    renderPage('tx-nonexistent');
    expect(await screen.findByText('Page not found')).toBeInTheDocument();
  });

  it('renders amount with sign and color', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/\u22125 TON/)).toBeInTheDocument();
    });
  });

  it('renders status badge', async () => {
    renderPage();
    expect(await screen.findByText('Confirmed')).toBeInTheDocument();
  });

  it('renders detail fields — type label and channel title', async () => {
    renderPage();
    expect(await screen.findByText('Escrow deposit')).toBeInTheDocument();
    expect(screen.getByText('Crypto News Daily')).toBeInTheDocument();
  });

  it('renders blockchain data when present (truncated addresses)', async () => {
    renderPage();
    // Wait for the detail to load
    await screen.findByText('Escrow deposit');
    // Truncated txHash: first 8 chars + ... + last 6 chars
    await waitFor(() => {
      expect(screen.getByText(/a1b2c3d4.*a1b2/)).toBeInTheDocument();
    });
  });

  it('hides blockchain section when no data', async () => {
    server.use(
      http.get(`${API_BASE}/wallet/transactions/:txId`, () => HttpResponse.json(mockTransactionDetailMinimal)),
    );
    renderPage('tx-3');
    await screen.findByText('Pending');
    // No truncated addresses should appear
    const page = document.body.textContent ?? '';
    expect(page).not.toContain('EQBvW8Z5');
  });

  it('renders TON Explorer link', async () => {
    renderPage();
    expect(await screen.findByText('View in TON Explorer')).toBeInTheDocument();
  });

  it('copies hash on click', async () => {
    const { user } = renderPage();
    // Wait for truncated hash to appear
    const hashEl = await screen.findByText(/a1b2c3d4.*a1b2/);
    // Click the parent GroupItem container
    const groupItem = hashEl.closest('[data-group-item]') as HTMLElement;
    expect(groupItem).toBeTruthy();
    await user.click(groupItem);
    expect(mockCopyToClipboard).toHaveBeenCalledWith(mockTransactionDetail.txHash);
  });
});
