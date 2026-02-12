import { HttpResponse, http } from 'msw';
import { mockAuthResponse, mockProfile } from './data';

const API_BASE = '/api/v1';

let profile = { ...mockProfile };

export const handlers = [
  // POST /auth/login — authenticate via Telegram initData
  http.post(`${API_BASE}/auth/login`, () => {
    sessionStorage.setItem('access_token', mockAuthResponse.accessToken);
    return HttpResponse.json(mockAuthResponse);
  }),

  // GET /profile — current user profile
  http.get(`${API_BASE}/profile`, () => {
    return HttpResponse.json(profile);
  }),

  // PUT /profile/onboarding — complete onboarding
  http.put(`${API_BASE}/profile/onboarding`, async ({ request }) => {
    const body = (await request.json()) as { interests: string[] };
    profile = {
      ...profile,
      onboardingCompleted: true,
      interests: body.interests,
    };
    return HttpResponse.json(profile);
  }),

  // GET /deals — empty list stub
  http.get(`${API_BASE}/deals`, () => {
    return HttpResponse.json({
      items: [],
      nextCursor: null,
      hasNext: false,
    });
  }),
];
