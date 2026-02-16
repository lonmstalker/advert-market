import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import i18n from 'i18next';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { renderHook, waitFor } from '@/test/test-utils';
import { useAuth } from './use-auth';

vi.mock('@/shared/api/auth', () => ({
  login: vi.fn(),
  fetchProfile: vi.fn(),
}));

vi.mock('@/shared/lib/telegram-init-data', () => ({
  getTelegramInitData: vi.fn(() => 'telegram-init-data'),
}));

import { fetchProfile, login } from '@/shared/api/auth';

function makeProfile(overrides: Record<string, unknown> = {}) {
  return {
    id: 1,
    telegramId: 1,
    username: 'user',
    displayName: 'User',
    languageCode: 'en',
    displayCurrency: 'USD',
    currencyMode: 'AUTO',
    notificationSettings: {
      deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
      financial: { deposits: true, payouts: true, escrow: true },
      disputes: { opened: true, resolved: true },
    },
    onboardingCompleted: true,
    interests: ['advertiser'],
    createdAt: '2026-02-15T00:00:00.000Z',
    ...overrides,
  };
}

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });

  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('useAuth', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    await i18n.changeLanguage('en');
    vi.mocked(fetchProfile).mockReset();
    vi.mocked(login).mockReset();
    vi.mocked(login).mockResolvedValue({ accessToken: 'token' } as never);

    useSettingsStore.setState({
      languageCode: 'en',
      displayCurrency: 'USD',
      currencyMode: 'AUTO',
      notificationSettings: {
        deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
        financial: { deposits: true, payouts: true, escrow: true },
        disputes: { opened: true, resolved: true },
      },
      isLoaded: false,
    });
  });

  it('syncs i18n language with profile language from backend', async () => {
    sessionStorage.setItem('access_token', 'existing-token');
    vi.mocked(fetchProfile).mockResolvedValue(makeProfile({ languageCode: 'ru' }) as never);

    const { result } = renderHook(() => useAuth(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.profile?.languageCode).toBe('ru');
    });

    await waitFor(() => {
      expect(i18n.language).toBe('ru');
    });
  });
});
