import { beforeEach, describe, expect, it } from 'vitest';
import type { NotificationSettings, UserProfile } from '@/shared/api/auth';
import { useSettingsStore } from '../settings-store';

const DEFAULT_NOTIFICATIONS: NotificationSettings = {
  deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
  financial: { deposits: true, payouts: true, escrow: true },
  disputes: { opened: true, resolved: true },
};

function makeProfile(overrides: Partial<UserProfile> = {}): UserProfile {
  return {
    id: 1,
    telegramId: 1,
    username: 'testuser',
    displayName: 'Test User',
    languageCode: 'en',
    displayCurrency: 'EUR',
    currencyMode: 'MANUAL',
    notificationSettings: {
      deals: { newOffers: false, acceptReject: true, deliveryStatus: false },
      financial: { deposits: true, payouts: false, escrow: true },
      disputes: { opened: false, resolved: true },
    },
    onboardingCompleted: true,
    interests: ['advertiser'],
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('useSettingsStore', () => {
  beforeEach(() => {
    useSettingsStore.setState({
      languageCode: 'en',
      displayCurrency: 'USD',
      currencyMode: 'AUTO',
      notificationSettings: DEFAULT_NOTIFICATIONS,
      isLoaded: false,
    });
  });

  it('has correct initial values', () => {
    const state = useSettingsStore.getState();
    expect(state.languageCode).toBe('en');
    expect(state.displayCurrency).toBe('USD');
    expect(state.currencyMode).toBe('AUTO');
    expect(state.isLoaded).toBe(false);
    expect(state.notificationSettings).toEqual(DEFAULT_NOTIFICATIONS);
  });

  it('setFromProfile sets all fields and marks as loaded', () => {
    const profile = makeProfile();
    useSettingsStore.getState().setFromProfile(profile);

    const state = useSettingsStore.getState();
    expect(state.languageCode).toBe('en');
    expect(state.displayCurrency).toBe('EUR');
    expect(state.currencyMode).toBe('MANUAL');
    expect(state.isLoaded).toBe(true);
    expect(state.notificationSettings).toEqual(profile.notificationSettings);
  });

  it('setFromProfile updates values on subsequent calls', () => {
    const profile1 = makeProfile({ displayCurrency: 'EUR' });
    useSettingsStore.getState().setFromProfile(profile1);
    expect(useSettingsStore.getState().displayCurrency).toBe('EUR');

    const profile2 = makeProfile({ displayCurrency: 'RUB' });
    useSettingsStore.getState().setFromProfile(profile2);
    expect(useSettingsStore.getState().displayCurrency).toBe('RUB');
  });

  it('setDisplayCurrency changes only currency', () => {
    useSettingsStore.getState().setDisplayCurrency('RUB');

    const state = useSettingsStore.getState();
    expect(state.languageCode).toBe('en');
    expect(state.displayCurrency).toBe('RUB');
    expect(state.currencyMode).toBe('AUTO');
    expect(state.isLoaded).toBe(false);
    expect(state.notificationSettings).toEqual(DEFAULT_NOTIFICATIONS);
  });

  it('setCurrencyMode changes only mode', () => {
    useSettingsStore.getState().setCurrencyMode('MANUAL');

    const state = useSettingsStore.getState();
    expect(state.languageCode).toBe('en');
    expect(state.currencyMode).toBe('MANUAL');
    expect(state.displayCurrency).toBe('USD');
    expect(state.notificationSettings).toEqual(DEFAULT_NOTIFICATIONS);
  });

  it('setLanguageCode changes only language', () => {
    useSettingsStore.getState().setLanguageCode('ru');

    const state = useSettingsStore.getState();
    expect(state.languageCode).toBe('ru');
    expect(state.currencyMode).toBe('AUTO');
    expect(state.displayCurrency).toBe('USD');
    expect(state.notificationSettings).toEqual(DEFAULT_NOTIFICATIONS);
  });

  it('setNotificationSetting performs deep merge correctly', () => {
    useSettingsStore.getState().setNotificationSetting('deals', 'newOffers', false);

    const state = useSettingsStore.getState();
    expect(state.notificationSettings.deals.newOffers).toBe(false);
    expect(state.notificationSettings.deals.acceptReject).toBe(true);
    expect(state.notificationSettings.deals.deliveryStatus).toBe(true);
  });

  it('setNotificationSetting accumulates multiple changes', () => {
    useSettingsStore.getState().setNotificationSetting('deals', 'newOffers', false);
    useSettingsStore.getState().setNotificationSetting('financial', 'payouts', false);
    useSettingsStore.getState().setNotificationSetting('disputes', 'resolved', false);

    const state = useSettingsStore.getState();
    expect(state.notificationSettings.deals.newOffers).toBe(false);
    expect(state.notificationSettings.financial.payouts).toBe(false);
    expect(state.notificationSettings.disputes.resolved).toBe(false);
    // untouched fields remain true
    expect(state.notificationSettings.deals.acceptReject).toBe(true);
    expect(state.notificationSettings.financial.deposits).toBe(true);
    expect(state.notificationSettings.disputes.opened).toBe(true);
  });
});
