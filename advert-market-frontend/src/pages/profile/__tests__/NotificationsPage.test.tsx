import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router';
import type { NotificationSettings } from '@/shared/api';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { server } from '@/test/mocks/server';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import NotificationsPage from '../NotificationsPage';

vi.mock('@/shared/hooks/use-toast', () => ({
  useToast: () => ({
    showToast: vi.fn(),
    showSuccess: vi.fn(),
    showError: mockShowError,
    showInfo: vi.fn(),
  }),
}));

const mockShowError = vi.fn();

const defaultNotifications: NotificationSettings = {
  deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
  financial: { deposits: true, payouts: true, escrow: true },
  disputes: { opened: true, resolved: true },
};

function renderPage(notifications = defaultNotifications) {
  useSettingsStore.setState({
    languageCode: 'en',
    notificationSettings: notifications,
    isLoaded: true,
  });

  return renderWithProviders(
    <Routes>
      <Route path="/profile/notifications" element={<NotificationsPage />} />
    </Routes>,
    { initialEntries: ['/profile/notifications'] },
  );
}

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    mockShowError.mockClear();
    sessionStorage.setItem('access_token', 'test-token');
  });

  afterEach(() => {
    vi.useRealTimers();
    sessionStorage.clear();
  });

  it('renders 3 groups: Deals, Financial, Disputes', () => {
    renderPage();
    expect(screen.getByText('Deals')).toBeInTheDocument();
    expect(screen.getByText('Financial')).toBeInTheDocument();
    expect(screen.getByText('Disputes')).toBeInTheDocument();
  });

  it('renders 8 toggle items with correct labels', () => {
    renderPage();
    // i18n: profile.notifications.*
    expect(screen.getByText('New offers')).toBeInTheDocument();
    expect(screen.getByText('Accept & reject')).toBeInTheDocument();
    expect(screen.getByText('Delivery status')).toBeInTheDocument();
    expect(screen.getByText('Deposits')).toBeInTheDocument();
    expect(screen.getByText('Payouts')).toBeInTheDocument();
    expect(screen.getByText('Escrow')).toBeInTheDocument();
    expect(screen.getByText('Dispute opened')).toBeInTheDocument();
    expect(screen.getByText('Dispute resolved')).toBeInTheDocument();
  });

  it('shows descriptions on key notification items', () => {
    renderPage();
    expect(screen.getByText('When someone wants to buy advertising')).toBeInTheDocument();
    expect(screen.getByText('Top-ups to your wallet')).toBeInTheDocument();
    expect(screen.getByText('Locking and releasing funds')).toBeInTheDocument();
  });

  it('toggle click instantly updates store', async () => {
    const { user } = renderPage();
    const toggles = screen.getAllByRole('switch');
    expect(toggles).toHaveLength(8);
    expect(toggles[0]).toBeChecked();

    await user.click(toggles[0]); // toggle newOffers off

    expect(useSettingsStore.getState().notificationSettings.deals.newOffers).toBe(false);
  });

  it('debounces mutations: fast clicks produce single mutation after 500ms', async () => {
    let mutationCount = 0;
    server.use(
      http.put('/api/v1/profile/settings', async () => {
        mutationCount++;
        return HttpResponse.json({
          id: 1,
          telegramId: 1,
          username: 'testuser',
          displayName: 'Test User',
          languageCode: 'en',
          displayCurrency: 'USD',
          currencyMode: 'AUTO',
          notificationSettings: useSettingsStore.getState().notificationSettings,
          onboardingCompleted: true,
          interests: [],
          createdAt: '2026-01-01T00:00:00Z',
        });
      }),
    );

    const { user } = renderPage();
    const toggles = screen.getAllByRole('switch');

    await user.click(toggles[0]); // toggle 1
    await user.click(toggles[1]); // toggle 2 — resets debounce timer

    expect(mutationCount).toBe(0);

    await vi.advanceTimersByTimeAsync(600);

    await waitFor(() => {
      expect(mutationCount).toBe(1);
    });
  });

  it('shows error toast on mutation failure', async () => {
    server.use(
      http.put('/api/v1/profile/settings', () => {
        return HttpResponse.json({ title: 'Server Error', status: 500 }, { status: 500 });
      }),
    );

    const { user } = renderPage();
    const toggles = screen.getAllByRole('switch');
    await user.click(toggles[0]);

    await vi.advanceTimersByTimeAsync(600);

    await waitFor(() => {
      expect(mockShowError).toHaveBeenCalled();
    });
  });
});
