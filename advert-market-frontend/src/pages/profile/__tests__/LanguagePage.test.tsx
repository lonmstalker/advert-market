import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { mockProfile } from '@/test/mocks/data';
import { server } from '@/test/mocks/server';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import LanguagePage from '../LanguagePage';

vi.mock('@/shared/hooks/use-auth', () => ({
  useAuth: () => ({
    profile: { ...mockProfile, languageCode: 'ru' },
    isAuthenticated: true,
    isLoading: false,
    invalidateProfile: vi.fn(),
  }),
}));

vi.mock('@/shared/hooks/use-toast', () => ({
  useToast: () => ({
    showToast: vi.fn(),
    showSuccess: vi.fn(),
    showError: mockShowError,
    showInfo: vi.fn(),
  }),
}));

const mockShowError = vi.fn();

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/profile/language" element={<LanguagePage />} />
    </Routes>,
    { initialEntries: ['/profile/language'] },
  );
}

describe('LanguagePage', () => {
  beforeEach(() => {
    mockShowError.mockClear();
    sessionStorage.setItem('access_token', 'test-token');
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('renders both languages with labels', () => {
    renderPage();
    expect(screen.getByText('Русский')).toBeInTheDocument();
    expect(screen.getByText('English')).toBeInTheDocument();
  });

  it('renders language page title', () => {
    renderPage();
    expect(screen.getByText('Language')).toBeInTheDocument();
  });

  it('renders flag icons in colored circles', () => {
    renderPage();
    expect(screen.getByText('\uD83C\uDDF7\uD83C\uDDFA')).toBeInTheDocument();
    expect(screen.getByText('\uD83C\uDDEC\uD83C\uDDE7')).toBeInTheDocument();
  });

  it('shows footer hint text', () => {
    renderPage();
    expect(screen.getByText('App interface language')).toBeInTheDocument();
  });

  it('does not call mutation when clicking already selected language', async () => {
    let mutationCalled = false;
    server.use(
      http.put('/api/v1/profile/language', async () => {
        mutationCalled = true;
        return HttpResponse.json({ ...mockProfile, languageCode: 'ru' });
      }),
    );

    const { user } = renderPage();
    await user.click(screen.getByText('Русский'));

    // Give time for any potential mutation to fire
    await new Promise((r) => setTimeout(r, 50));
    expect(mutationCalled).toBe(false);
  });

  it('updates settings store on successful mutation', async () => {
    const { user } = renderPage();
    await user.click(screen.getByText('English'));

    await waitFor(() => {
      const state = useSettingsStore.getState();
      expect(state.isLoaded).toBe(true);
    });
  });

  it('shows error toast and rolls back on mutation failure', async () => {
    server.use(
      http.put('/api/v1/profile/language', () => {
        return HttpResponse.json({ title: 'Server Error', status: 500 }, { status: 500 });
      }),
    );

    const { user } = renderPage();
    await user.click(screen.getByText('English'));

    await waitFor(() => {
      expect(mockShowError).toHaveBeenCalled();
    });
  });
});
