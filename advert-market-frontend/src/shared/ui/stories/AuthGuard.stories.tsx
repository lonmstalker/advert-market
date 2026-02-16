import type { Decorator, Meta, StoryObj } from '@storybook/react-vite';
import { Text } from '@telegram-tools/ui-kit';
import type { ReactNode } from 'react';
import { useEffect } from 'react';
import { Route, Routes } from 'react-router';
import { AuthGuard } from '../components/auth-guard';

type AuthScenario = 'loading' | 'unauthorized' | 'authorized' | 'onboarding';

type UserProfilePayload = {
  id: number;
  telegramId: number;
  username: string;
  displayName: string;
  languageCode: 'en';
  displayCurrency: 'USD';
  currencyMode: 'AUTO';
  notificationSettings: {
    deals: { newOffers: true; acceptReject: true; deliveryStatus: true };
    financial: { deposits: true; payouts: true; escrow: true };
    disputes: { opened: true; resolved: true };
  };
  onboardingCompleted: boolean;
  interests: string[];
  createdAt: string;
};

const apiProblem = (title: string, status: number, detail: string) => ({
  type: 'about:blank',
  title,
  status,
  detail,
});

const createProfile = (onboardingCompleted: boolean): UserProfilePayload => ({
  id: 101,
  telegramId: 101,
  username: 'storybook-user',
  displayName: 'Storybook User',
  languageCode: 'en',
  displayCurrency: 'USD',
  currencyMode: 'AUTO',
  notificationSettings: {
    deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
    financial: { deposits: true, payouts: true, escrow: true },
    disputes: { opened: true, resolved: true },
  },
  onboardingCompleted,
  interests: onboardingCompleted ? ['advertiser'] : [],
  createdAt: '2026-02-16T00:00:00Z',
});

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function createFetchMock(scenario: AuthScenario, fallbackFetch: typeof window.fetch): typeof window.fetch {
  return async (input, init) => {
    const rawUrl = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;

    const path = new URL(rawUrl, window.location.origin).pathname;
    if (!path.startsWith('/api/v1/')) {
      return fallbackFetch(input, init);
    }

    if (path === '/api/v1/auth/login') {
      if (scenario === 'unauthorized') {
        return jsonResponse(apiProblem('Unauthorized', 401, 'Storybook auth fallback'), 401);
      }

      return jsonResponse(
        {
          accessToken: 'storybook-token',
          expiresIn: 3600,
          user: {
            id: 101,
            username: 'storybook-user',
            displayName: 'Storybook User',
          },
        },
        200,
      );
    }

    if (path === '/api/v1/profile') {
      if (scenario === 'loading') {
        return new Promise<Response>(() => {});
      }

      if (scenario === 'authorized') {
        return jsonResponse(createProfile(true), 200);
      }

      if (scenario === 'onboarding') {
        return jsonResponse(createProfile(false), 200);
      }

      return jsonResponse(apiProblem('Unauthorized', 401, 'Profile is unavailable'), 401);
    }

    return jsonResponse(apiProblem('Not Found', 404, `No mocked handler for ${path}`), 404);
  };
}

function setupScenario(scenario: AuthScenario): () => void {
  const previousFetch = window.fetch;
  const previousToken = sessionStorage.getItem('access_token');

  if (scenario === 'unauthorized') {
    sessionStorage.removeItem('access_token');
  } else {
    sessionStorage.setItem('access_token', 'storybook-token');
  }

  window.fetch = createFetchMock(scenario, previousFetch);

  return () => {
    window.fetch = previousFetch;
    if (previousToken) {
      sessionStorage.setItem('access_token', previousToken);
    } else {
      sessionStorage.removeItem('access_token');
    }
  };
}

function SetupCleanup({ cleanup, children }: { cleanup: () => void; children: ReactNode }) {
  useEffect(() => cleanup, [cleanup]);
  return <>{children}</>;
}

function withAuthScenario(scenario: AuthScenario): Decorator {
  return (Story) => {
    const cleanup = setupScenario(scenario);
    return (
      <SetupCleanup cleanup={cleanup}>
        <Story />
      </SetupCleanup>
    );
  };
}

function AuthGuardPreview() {
  return (
    <Routes>
      <Route element={<AuthGuard />}>
        <Route
          index
          element={
            <div style={{ minHeight: '50vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Text type="title3" weight="medium">
                Protected outlet content
              </Text>
            </div>
          }
        />
      </Route>
      <Route
        path="/onboarding"
        element={
          <div style={{ minHeight: '50vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Text type="title3" weight="medium">
              Onboarding route
            </Text>
          </div>
        }
      />
    </Routes>
  );
}

const meta: Meta<typeof AuthGuardPreview> = {
  title: 'Ad Market/Infrastructure/AuthGuard',
  component: AuthGuardPreview,
  parameters: {
    route: '/',
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const LoadingState: Story = {
  decorators: [withAuthScenario('loading')],
};

export const UnauthorizedState: Story = {
  decorators: [withAuthScenario('unauthorized')],
};

export const AuthorizedState: Story = {
  decorators: [withAuthScenario('authorized')],
};

export const OnboardingRedirectState: Story = {
  decorators: [withAuthScenario('onboarding')],
};
