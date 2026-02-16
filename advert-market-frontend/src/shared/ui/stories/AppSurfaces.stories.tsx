import type { Meta, StoryObj } from '@storybook/react-vite';
import { BadgeCheck } from 'lucide-react';
import { Button, Text } from '@telegram-tools/ui-kit';
import { AppIcon } from '../components/app-icon';
import { AppListRow } from '../components/app-list-row';
import { AppSectionHeader } from '../components/app-section-header';
import { AppSurfaceCard } from '../components/app-surface-card';
import { ChannelAvatar } from '../components/channel-avatar';
import { EndOfList } from '../components/end-of-list';
import { FormattedPrice } from '../components/formatted-price';
import { LanguageBadge } from '../components/language-badge';
import { MegaphoneIcon } from '../icons';

const meta: Meta = {
  title: 'Ad Market/Surfaces',
  parameters: {
    route: '/catalog',
  },
};

export default meta;
type Story = StoryObj;

export const SectionAndRows: Story = {
  render: () => (
    <div style={{ display: 'grid', gap: 16 }}>
      <AppSectionHeader
        title="Популярные каналы"
        subtitle="Приоритетные размещения"
        action={<Button text="Смотреть все" type="secondary" />}
      />

      <AppSurfaceCard>
        <AppListRow
          label="Crypto News Daily"
          description="Crypto · 125K подписчиков"
          before={<ChannelAvatar title="Crypto News Daily" badge={<BadgeCheck size={14} />} />}
          value={<FormattedPrice nanoTon={5_000_000_000} showFiat={false} />}
          chevron
        />
        <AppListRow
          label="Tech Digest"
          description="Tech · 89K подписчиков"
          before={<ChannelAvatar title="Tech Digest" />}
          value={<FormattedPrice nanoTon={3_000_000_000} showFiat={false} />}
          chevron
        />
      </AppSurfaceCard>

      <EndOfList label="Показаны лучшие варианты" />
    </div>
  ),
};

export const PrimitiveWidgets: Story = {
  render: () => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
      <AppIcon icon={MegaphoneIcon} size={20} className="am-onboarding-card-icon" />
      <LanguageBadge code="RU" />
      <LanguageBadge code="EN" size="sm" />
      <div style={{ display: 'inline-flex', alignItems: 'baseline', gap: 6 }}>
        <Text type="caption1" color="secondary">
          from
        </Text>
        <FormattedPrice nanoTon={8_000_000_000} showFiat={false} size="sm" />
      </div>
    </div>
  ),
};
