import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { formatCompactNumber } from '@/shared/lib/format-number';
import { ChannelAvatar } from '@/shared/ui/components/channel-avatar';
import type { Channel, ChannelDetail } from '../types/channel';

type ChannelCardProps = {
  channel: Channel | ChannelDetail;
};

export function ChannelCard({ channel }: ChannelCardProps) {
  const { t } = useTranslation();

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: 12,
        borderRadius: 12,
        border: '1px solid var(--color-border-separator)',
        background: 'var(--color-background-base)',
      }}
    >
      <ChannelAvatar title={channel.title} size="sm" />
      <div style={{ minWidth: 0 }}>
        <Text type="body" weight="medium">
          {channel.title}
        </Text>
        <Text type="subheadline2" color="secondary">
          {formatCompactNumber(channel.subscriberCount)} {t('catalog.channel.subscribers', { count: '' }).trim()}
        </Text>
      </div>
    </div>
  );
}
