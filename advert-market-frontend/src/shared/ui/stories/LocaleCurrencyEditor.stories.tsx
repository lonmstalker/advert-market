import type { Meta, StoryObj } from '@storybook/react-vite';
import { useSettingsStore } from '@/shared/stores/settings-store';
import { LocaleCurrencyEditor } from '../components/locale-currency-editor';

const baseSettings = {
  languageCode: 'ru',
  displayCurrency: 'RUB',
  currencyMode: 'AUTO' as const,
  isLoaded: true,
  notificationSettings: {
    deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
    financial: { deposits: true, payouts: true, escrow: true },
    disputes: { opened: true, resolved: true },
  },
};

function seedSettings(overrides?: Partial<typeof baseSettings>) {
  useSettingsStore.setState({
    ...baseSettings,
    ...overrides,
  });
}

const meta: Meta<typeof LocaleCurrencyEditor> = {
  title: 'Ad Market/Locale Currency Editor',
  component: LocaleCurrencyEditor,
};

export default meta;
type Story = StoryObj<typeof LocaleCurrencyEditor>;

export const OnboardingMode: Story = {
  render: (args) => {
    seedSettings({ currencyMode: 'AUTO', displayCurrency: 'RUB' });
    return <LocaleCurrencyEditor {...args} />;
  },
  args: {
    mode: 'onboarding',
  },
};

export const ProfileModeManualCurrency: Story = {
  render: (args) => {
    seedSettings({ currencyMode: 'MANUAL', displayCurrency: 'USD' });
    return <LocaleCurrencyEditor {...args} />;
  },
  args: {
    mode: 'profile',
  },
};
