import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button, Text } from '@telegram-tools/ui-kit';
import { AppListRow } from '../components/app-list-row';
import { AppPageShell } from '../components/app-page-shell';
import { AppSectionHeader } from '../components/app-section-header';
import { AppSurfaceCard } from '../components/app-surface-card';
import { ChannelAvatar } from '../components/channel-avatar';

const meta: Meta<typeof AppPageShell> = {
  title: 'Ad Market/App Shell',
  component: AppPageShell,
  parameters: {
    route: '/catalog',
  },
};

export default meta;
type Story = StoryObj<typeof AppPageShell>;

export const CatalogShell: Story = {
  render: () => (
    <AppPageShell withTabsPadding={false} testId="storybook-app-shell-catalog">
      <div style={{ display: 'grid', gap: 16 }}>
        <AppSectionHeader
          title="Каталог каналов"
          subtitle="Подобранные каналы по вашим категориям"
          action={<Button text="Фильтры" type="secondary" />}
        />

        <AppSurfaceCard>
          <AppListRow
            label="Crypto News Daily"
            description="Крипто · 125K подписчиков"
            before={<ChannelAvatar title="Crypto News Daily" size="md" />}
            value={
              <Text type="callout" weight="bold" color="accent">
                from 5 TON
              </Text>
            }
            chevron
          />
          <AppListRow
            label="Tech Digest"
            description="Технологии · 89K подписчиков"
            before={<ChannelAvatar title="Tech Digest" size="md" />}
            value={
              <Text type="callout" weight="bold" color="accent">
                from 3 TON
              </Text>
            }
            chevron
          />
        </AppSurfaceCard>
      </div>
    </AppPageShell>
  ),
};

export const FinanceShell: Story = {
  parameters: {
    route: '/wallet',
  },
  render: () => (
    <AppPageShell variant="finance" withTabsPadding={false} testId="storybook-app-shell-finance">
      <div style={{ display: 'grid', gap: 16 }}>
        <AppSectionHeader title="Финансы" subtitle="Escrow и расчеты по сделкам" />
        <AppSurfaceCard>
          <div style={{ display: 'grid', gap: 8 }}>
            <Text type="caption1" color="secondary">
              Доступный баланс
            </Text>
            <Text type="hero" weight="bold">
              15 TON
            </Text>
            <Text type="caption1" color="tertiary">
              В эскроу: 5 TON
            </Text>
          </div>
        </AppSurfaceCard>
      </div>
    </AppPageShell>
  ),
};
