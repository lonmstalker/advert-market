import { HttpResponse, http } from 'msw';
import { mockAuthResponse, mockProfile } from '@/test/mocks/data';
import { server } from '@/test/mocks/server';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

import { completeOnboarding, fetchProfile, login } from './auth-api';

describe('auth-api', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('login returns auth response', async () => {
    const response = await login('test-init-data');
    expect(response.accessToken).toBe(mockAuthResponse.accessToken);
    expect(response.user.username).toBe(mockAuthResponse.user.username);
  });

  it('login stores token in sessionStorage', async () => {
    await login('test-init-data');
    expect(sessionStorage.getItem('access_token')).toBe(mockAuthResponse.accessToken);
  });

  it('fetchProfile returns user profile', async () => {
    sessionStorage.setItem('access_token', 'test-token');
    const profile = await fetchProfile();
    expect(profile.username).toBe(mockProfile.username);
    expect(profile.telegramId).toBe(mockProfile.telegramId);
  });

  it('completeOnboarding returns updated profile', async () => {
    sessionStorage.setItem('access_token', 'test-token');
    const profile = await completeOnboarding(['advertiser']);
    expect(profile.onboardingCompleted).toBe(true);
    expect(profile.interests).toContain('advertiser');
  });

  it('login throws on server error', async () => {
    server.use(
      http.post('/api/v1/auth/login', () => {
        return HttpResponse.json({ type: 'about:blank', title: 'Internal Server Error', status: 500 }, { status: 500 });
      }),
    );

    await expect(login('test-init-data')).rejects.toThrow('Internal Server Error');
  });
});
