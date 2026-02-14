import { Route, Routes } from 'react-router';
import { renderWithProviders, screen } from '@/test/test-utils';
import { AuthGuard } from './auth-guard';

vi.mock('@/shared/hooks/use-auth', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '@/shared/hooks/use-auth';

const mockUseAuth = vi.mocked(useAuth);

describe('AuthGuard', () => {
  it('renders spinner when loading', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
      authError: null,
      retryAuth: vi.fn(),
      profile: null,
      invalidateProfile: vi.fn(),
    });
    renderWithProviders(
      <Routes>
        <Route element={<AuthGuard />}>
          <Route index element={<div>protected</div>} />
        </Route>
      </Routes>,
    );
    expect(screen.queryByText('protected')).not.toBeInTheDocument();
  });

  it('renders error state when not authenticated', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isLoading: false,
      authError: new Error('initData is missing'),
      retryAuth: vi.fn(),
      profile: null,
      invalidateProfile: vi.fn(),
    });
    renderWithProviders(
      <Routes>
        <Route element={<AuthGuard />}>
          <Route index element={<div>protected</div>} />
        </Route>
      </Routes>,
    );
    expect(screen.queryByText('protected')).not.toBeInTheDocument();
    expect(screen.getByText("Couldn't sign in")).toBeInTheDocument();
  });

  it('redirects to /onboarding when onboarding not completed', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      authError: null,
      retryAuth: vi.fn(),
      profile: {
        id: 1,
        telegramId: 123456789,
        username: 'test',
        displayName: 'Test',
        languageCode: 'en',
        onboardingCompleted: false,
        interests: [],
        createdAt: '2026-01-01T00:00:00Z',
      },
      invalidateProfile: vi.fn(),
    });
    renderWithProviders(
      <Routes>
        <Route element={<AuthGuard />}>
          <Route index element={<div>protected</div>} />
        </Route>
        <Route path="/onboarding" element={<div>onboarding</div>} />
      </Routes>,
    );
    expect(screen.getByText('onboarding')).toBeInTheDocument();
    expect(screen.queryByText('protected')).not.toBeInTheDocument();
  });

  it('renders outlet when authenticated with completed onboarding', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      authError: null,
      retryAuth: vi.fn(),
      profile: {
        id: 1,
        telegramId: 123456789,
        username: 'test',
        displayName: 'Test',
        languageCode: 'en',
        onboardingCompleted: true,
        interests: ['advertiser'],
        createdAt: '2026-01-01T00:00:00Z',
      },
      invalidateProfile: vi.fn(),
    });
    renderWithProviders(
      <Routes>
        <Route element={<AuthGuard />}>
          <Route index element={<div>protected</div>} />
        </Route>
      </Routes>,
    );
    expect(screen.getByText('protected')).toBeInTheDocument();
  });
});
