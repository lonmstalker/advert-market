import { HttpResponse, http } from 'msw';
import { mockProfile } from '@/test/mocks/data';
import { server } from '@/test/mocks/server';
import { updateLanguage, updateSettings } from '../profile-api';

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

    it('throws on server error', async () => {
      server.use(
        http.put('/api/v1/profile/settings', () => {
          return HttpResponse.json({ title: 'Internal Server Error', status: 500 }, { status: 500 });
        }),
      );

      await expect(updateSettings({ displayCurrency: 'RUB' })).rejects.toThrow();
    });
  });
});
