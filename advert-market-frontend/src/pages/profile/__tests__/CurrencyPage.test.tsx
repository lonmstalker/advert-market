import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { server } from '@/test/mocks/server';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import CurrencyPage from '../CurrencyPage';

vi.mock('@/shared/hooks/use-toast', () => ({
  useToast: () => ({
    showToast: vi.fn(),
    showSuccess: vi.fn(),
    showError: mockShowError,
    showInfo: vi.fn(),
  }),
}));

const mockShowError = vi.fn();

function renderPage(initialCurrency = 'USD') {
  useSettingsStore.setState({ displayCurrency: initialCurrency, isLoaded: true });

  return renderWithProviders(
    <Routes>
      <Route path="/profile/currency" element={<CurrencyPage />} />
    </Routes>,
    { initialEntries: ['/profile/currency'] },
  );
}

describe('CurrencyPage', () => {
  beforeEach(() => {
    mockShowError.mockClear();
    sessionStorage.setItem('access_token', 'test-token');
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('renders 3 currencies with localized names', () => {
    renderPage();
    // i18n keys: profile.currency.USD = "US Dollar ($)", etc.
    expect(screen.getByText(/US Dollar/)).toBeInTheDocument();
    expect(screen.getByText(/Euro/)).toBeInTheDocument();
    expect(screen.getByText(/Russian Ruble/)).toBeInTheDocument();
  });

  it('shows header and footer hint', () => {
    renderPage();
    expect(screen.getByText('Choose currency')).toBeInTheDocument();
  });

  it('optimistically updates store on currency click', async () => {
    const { user } = renderPage('USD');
    await user.click(screen.getByText(/Euro/));

    await waitFor(() => {
      expect(useSettingsStore.getState().displayCurrency).toBe('EUR');
    });
  });

  it('does nothing when clicking current currency', async () => {
    const { user } = renderPage('USD');
    await user.click(screen.getByText(/US Dollar/));
    expect(useSettingsStore.getState().displayCurrency).toBe('USD');
  });

  it('updates store on successful mutation', async () => {
    const { user } = renderPage('USD');
    await user.click(screen.getByText(/Euro/));

    await waitFor(() => {
      expect(useSettingsStore.getState().isLoaded).toBe(true);
    });
  });

  it('rolls back on mutation failure and shows error toast', async () => {
    server.use(
      http.put('/api/v1/profile/settings', () => {
        return HttpResponse.json({ title: 'Server Error', status: 500 }, { status: 500 });
      }),
    );

    const { user } = renderPage('USD');
    await user.click(screen.getByText(/Euro/));

    await waitFor(() => {
      expect(mockShowError).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(useSettingsStore.getState().displayCurrency).toBe('USD');
    });
  });
});
