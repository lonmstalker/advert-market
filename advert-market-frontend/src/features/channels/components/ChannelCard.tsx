import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import type { Channel, ChannelDetail } from '../types/channel';

type ChannelCardProps = {
  channel: Channel | ChannelDetail;
};

function formatSubscribers(count: number): string {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`;
  if (count >= 1_000) return `${Math.round(count / 1_000)}K`;
  return String(count);
}

export function ChannelCard({ channel }: ChannelCardProps) {
  const { t } = useTranslation();
  const letter = channel.title.charAt(0).toUpperCase();
  const hue = (channel.title.charCodeAt(0) * 37 + (channel.title.charCodeAt(1) || 0) * 53) % 360;

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
      <div
        style={{
          width: 40,
          height: 40,
          borderRadius: '50%',
          background: `hsl(${hue}, 55%, 55%)`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}
      >
        <span style={{ color: '#fff', fontSize: 16, fontWeight: 600, lineHeight: 1 }}>{letter}</span>
      </div>
      <div style={{ minWidth: 0 }}>
        <Text type="body" weight="medium">
          {channel.title}
        </Text>
        <Text type="subheadline2" color="secondary">
          {formatSubscribers(channel.subscriberCount)} {t('catalog.channel.subscribers', { count: '' }).trim()}
        </Text>
      </div>
    </div>
  );
}
