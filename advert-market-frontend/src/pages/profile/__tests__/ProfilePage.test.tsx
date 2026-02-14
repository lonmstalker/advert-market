import { Route, Routes } from 'react-router';
import { renderWithProviders, screen } from '@/test/test-utils';
import ProfilePage from '../ProfilePage';

let currentProfile = {
  id: 1,
  telegramId: 123,
  username: 'testuser',
  displayName: 'Test User',
  languageCode: 'ru',
  displayCurrency: 'USD',
  notificationSettings: {
    deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
    financial: { deposits: true, payouts: true, escrow: true },
    disputes: { opened: true, resolved: true },
  },
  onboardingCompleted: true,
  interests: ['advertiser'],
  createdAt: '2026-01-01T00:00:00Z',
};

vi.mock('@/shared/hooks/use-auth', () => ({
  useAuth: () => ({
    profile: currentProfile,
    isAuthenticated: true,
    isLoading: false,
    invalidateProfile: vi.fn(),
  }),
}));

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="/profile/language" element={<div>language-page</div>} />
      <Route path="/profile/currency" element={<div>currency-page</div>} />
      <Route path="/profile/notifications" element={<div>notifications-page</div>} />
      <Route path="/profile/channels/new" element={<div>add-channel-page</div>} />
    </Routes>,
    { initialEntries: ['/profile'] },
  );
}

describe('ProfilePage', () => {
  beforeEach(() => {
    currentProfile = {
      id: 1,
      telegramId: 123,
      username: 'testuser',
      displayName: 'Test User',
      languageCode: 'ru',
      displayCurrency: 'USD',
      notificationSettings: {
        deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
        financial: { deposits: true, payouts: true, escrow: true },
        disputes: { opened: true, resolved: true },
      },
      onboardingCompleted: true,
      interests: ['advertiser'],
      createdAt: '2026-01-01T00:00:00Z',
    };
  });

  it('renders title "Profile"', () => {
    renderPage();
    expect(screen.getByText('Profile')).toBeInTheDocument();
  });

  it('shows displayName and @username', () => {
    renderPage();
    expect(screen.getByText('Test User')).toBeInTheDocument();
    expect(screen.getByText('@testuser')).toBeInTheDocument();
  });

  it('shows "U" avatar when displayName is empty', () => {
    currentProfile = { ...currentProfile, displayName: '' };
    renderPage();
    expect(screen.getByText('U')).toBeInTheDocument();
  });

  it('does not show @ prefix when username is null', () => {
    currentProfile = { ...currentProfile, username: null as unknown as string };
    renderPage();
    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
  });

  it('shows EmptyState with "Add channel" CTA', () => {
    renderPage();
    expect(screen.getByText('Add channel')).toBeInTheDocument();
  });

  it('shows current language label from profile.languageCode', () => {
    renderPage();
    // ProfilePage uses LANGUAGE_LABELS map, 'ru' → 'Русский'
    expect(screen.getByText('Русский')).toBeInTheDocument();
  });

  it('shows current currency from settings store', () => {
    renderPage();
    expect(screen.getByText('$ USD')).toBeInTheDocument();
  });

  it('navigates to /profile/language on Language click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByText('Language'));
    expect(screen.getByText('language-page')).toBeInTheDocument();
  });

  it('navigates to /profile/currency on Display Currency click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByText('Display Currency'));
    expect(screen.getByText('currency-page')).toBeInTheDocument();
  });

  it('navigates to /profile/notifications on Notifications click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByText('Notifications'));
    expect(screen.getByText('notifications-page')).toBeInTheDocument();
  });

  it('navigates to /profile/channels/new on Add channel CTA click', async () => {
    const { user } = renderPage();
    await user.click(screen.getByText('Add channel'));
    expect(screen.getByText('add-channel-page')).toBeInTheDocument();
  });
});
