import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { server } from '@/test/mocks/server';
import { renderWithProviders, screen } from '@/test/test-utils';
import { PaymentProvider } from './PaymentContext';
import { PaymentSheetContent } from './PaymentSheet';

const API_BASE = '/api/v1';

const mockSend = vi.fn();
const mockUseTonWalletStatus = vi.fn();

vi.mock('@/shared/hooks/use-toast', () => ({
  useToast: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showInfo: vi.fn(),
    showToast: vi.fn(),
  }),
}));

vi.mock('@tonconnect/ui-react', () => ({
  TonConnectButton: () => <div data-testid="ton-connect-button" />,
}));

vi.mock('@/shared/ton/hooks/useTonWalletStatus', () => ({
  useTonWalletStatus: () => mockUseTonWalletStatus(),
}));

vi.mock('@/shared/ton/hooks/useTonTransaction', () => ({
  useTonTransaction: () => ({ send: mockSend, isPending: false, error: null }),
}));

function renderSheet(options: { dealId?: string; onClose?: () => void } = {}) {
  const dealId = options.dealId ?? 'deal-3';
  const onClose = options.onClose ?? vi.fn();

  return {
    ...renderWithProviders(
      <PaymentProvider dealId={dealId} onClose={onClose}>
        <PaymentSheetContent />
      </PaymentProvider>,
    ),
    onClose,
  };
}

describe('PaymentSheet', () => {
  beforeEach(() => {
    mockSend.mockReset();
    mockUseTonWalletStatus.mockReset();
    sessionStorage.clear();
  });

  it('renders TonConnectButton and disables Pay when wallet is not connected', async () => {
    mockUseTonWalletStatus.mockReturnValue({
      isConnected: false,
      isConnectionRestored: true,
      address: null,
      friendlyAddress: null,
      wallet: null,
      connect: vi.fn(),
      disconnect: vi.fn(),
    });

    server.use(
      http.get(`${API_BASE}/deals/deal-3/deposit`, () =>
        HttpResponse.json({
          escrowAddress: 'UQ_ESCROW',
          amountNano: '8000000000',
          dealId: 'deal-3',
          status: 'AWAITING_PAYMENT',
          currentConfirmations: null,
          requiredConfirmations: null,
          receivedAmountNano: null,
          txHash: null,
          expiresAt: null,
        }),
      ),
    );

    renderSheet();
    expect(await screen.findByTestId('ton-connect-button')).toBeInTheDocument();

    const payButton = await screen.findByRole('button', { name: /pay/i });
    expect(payButton).toBeDisabled();
  });

  it('calls sendTransaction with escrow address and amount', async () => {
    mockUseTonWalletStatus.mockReturnValue({
      isConnected: true,
      isConnectionRestored: true,
      address: 'UQ_TEST',
      friendlyAddress: 'UQ_TEST',
      wallet: { account: { address: 'UQ_TEST' } },
      connect: vi.fn(),
      disconnect: vi.fn(),
    });

    server.use(
      http.get(`${API_BASE}/deals/deal-3/deposit`, () =>
        HttpResponse.json({
          escrowAddress: 'UQ_ESCROW',
          amountNano: '8000000000',
          dealId: 'deal-3',
          status: 'AWAITING_PAYMENT',
          currentConfirmations: null,
          requiredConfirmations: null,
          receivedAmountNano: null,
          txHash: null,
          expiresAt: null,
        }),
      ),
    );

    const { user } = renderSheet();
    const payButton = await screen.findByRole('button', { name: /pay/i });
    expect(payButton).toBeEnabled();

    await user.click(payButton);

    expect(mockSend).toHaveBeenCalledWith(
      expect.objectContaining({
        address: 'UQ_ESCROW',
        amountNano: '8000000000',
      }),
    );
  });

  it('closes the sheet when deposit is confirmed and pending intent exists', async () => {
    mockUseTonWalletStatus.mockReturnValue({
      isConnected: false,
      isConnectionRestored: true,
      address: null,
      friendlyAddress: null,
      wallet: null,
      connect: vi.fn(),
      disconnect: vi.fn(),
    });

    const onClose = vi.fn();
    sessionStorage.setItem(
      'ton_pending_intent',
      JSON.stringify({
        type: 'escrow_deposit',
        dealId: 'deal-3',
        sentAt: Date.now(),
        address: 'UQ_ESCROW',
        amountNano: '8000000000',
      }),
    );

    server.use(
      http.get(`${API_BASE}/deals/deal-3/deposit`, () =>
        HttpResponse.json({
          escrowAddress: 'UQ_ESCROW',
          amountNano: '8000000000',
          dealId: 'deal-3',
          status: 'CONFIRMED',
          currentConfirmations: 1,
          requiredConfirmations: 1,
          receivedAmountNano: '8000000000',
          txHash: 'txhash',
          expiresAt: null,
        }),
      ),
    );

    renderSheet({ onClose });

    // Wait for close side-effect
    await screen.findByTestId('ton-connect-button');
    expect(onClose).toHaveBeenCalled();
    expect(sessionStorage.getItem('ton_pending_intent')).toBeNull();
  });
});
