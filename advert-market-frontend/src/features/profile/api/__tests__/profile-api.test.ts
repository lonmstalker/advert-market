import { HttpResponse, http } from 'msw';
import { mockProfile } from '@/test/mocks/data';
import { server } from '@/test/mocks/server';
import { updateLanguage, updateSettings, updateWallet } from '../profile-api';

describe('profile-api', () => {
  beforeEach(() => {
    sessionStorage.setItem('access_token', 'test-token');
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  describe('updateLanguage', () => {
    it('sends PUT /profile/language and returns updated profile', async () => {
      const profile = await updateLanguage('en');
      expect(profile.languageCode).toBe('en');
      expect(profile.username).toBe(mockProfile.username);
    });

    it('throws on server error', async () => {
      server.use(
        http.put('/api/v1/profile/language', () => {
          return HttpResponse.json({ title: 'Internal Server Error', status: 500 }, { status: 500 });
        }),
      );

      await expect(updateLanguage('en')).rejects.toThrow();
    });
  });

  describe('updateSettings', () => {
    it('sends PUT /profile/settings with displayCurrency', async () => {
      const profile = await updateSettings({ displayCurrency: 'RUB' });
      expect(profile.displayCurrency).toBe('RUB');
      expect(profile.currencyMode).toBe('MANUAL');
    });

    it('sends PUT /profile/settings with notificationSettings', async () => {
      const notifications = {
        deals: { newOffers: false, acceptReject: true, deliveryStatus: true },
        financial: { deposits: true, payouts: false, escrow: true },
        disputes: { opened: true, resolved: false },
      };

      const profile = await updateSettings({ notificationSettings: notifications });
      expect(profile.notificationSettings).toEqual(notifications);
    });

    it('sends PUT /profile/settings with currencyMode AUTO', async () => {
      const profile = await updateSettings({ currencyMode: 'AUTO' });
      expect(profile.currencyMode).toBe('AUTO');
    });

    it('throws on server error', async () => {
      server.use(
        http.put('/api/v1/profile/settings', () => {
          return HttpResponse.json({ title: 'Internal Server Error', status: 500 }, { status: 500 });
        }),
      );

      await expect(updateSettings({ displayCurrency: 'RUB' })).rejects.toThrow();
    });
  });

  describe('updateWallet', () => {
    it('sends PUT /profile/wallet with tonAddress', async () => {
      let capturedTonAddress: string | null = null;
      server.use(
        http.put('/api/v1/profile/wallet', async ({ request }) => {
          const body = (await request.json()) as { tonAddress: string };
          capturedTonAddress = body.tonAddress;
          return HttpResponse.json({ ...mockProfile, tonAddress: body.tonAddress });
        }),
      );

      const profile = await updateWallet('UQBx7fEd1KyD5MHoDNFnVSXxwAAAAAABBBBBBBBBBBBBBBBB');
      expect(capturedTonAddress).toBe('UQBx7fEd1KyD5MHoDNFnVSXxwAAAAAABBBBBBBBBBBBBBBBB');
      expect(profile.tonAddress).toBe('UQBx7fEd1KyD5MHoDNFnVSXxwAAAAAABBBBBBBBBBBBBBBBB');
    });
  });
});
