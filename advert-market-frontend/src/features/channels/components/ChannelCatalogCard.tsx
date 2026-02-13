import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { formatTon } from '@/shared/lib/ton-format';
import { listItem, pressScale } from '@/shared/ui/animations';
import type { Channel } from '../types/channel';

type ChannelCatalogCardProps = {
  channel: Channel;
  onClick: () => void;
};

function formatSubscribers(count: number): string {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`;
  if (count >= 1_000) return `${Math.round(count / 1_000)}K`;
  return String(count);
}

function ChannelAvatar({ title }: { title: string }) {
  const letter = title.charAt(0).toUpperCase();
  const hue = (title.charCodeAt(0) * 37 + (title.charCodeAt(1) || 0) * 53) % 360;

  return (
    <div
      style={{
        width: 44,
        height: 44,
        borderRadius: '50%',
        background: `hsl(${hue}, 55%, 55%)`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
      }}
    >
      <span style={{ color: 'var(--color-static-white)', fontSize: 18, fontWeight: 600, lineHeight: 1 }}>{letter}</span>
    </div>
  );
}

function TopicBadge({ name }: { name: string }) {
  return (
    <span
      style={{
        padding: '2px 10px',
        borderRadius: 10,
        background: 'var(--color-background-secondary)',
        fontSize: 12,
        fontWeight: 500,
        color: 'var(--color-foreground-secondary)',
        whiteSpace: 'nowrap',
      }}
    >
      {name}
    </span>
  );
}

export function ChannelCatalogCard({ channel, onClick }: ChannelCatalogCardProps) {
  const topicName = channel.category ? channel.category.charAt(0).toUpperCase() + channel.category.slice(1) : null;

  return (
    <motion.div
      {...listItem}
      {...pressScale}
      onClick={onClick}
      style={{
        background: 'var(--color-background-base)',
        border: '1px solid var(--color-border-separator)',
        borderRadius: 16,
        padding: 16,
        cursor: 'pointer',
        WebkitTapHighlightColor: 'transparent',
      }}
    >
      {/* Row 1: avatar + title + price */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <ChannelAvatar title={channel.title} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <Text type="body" weight="bold">
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
              {channel.title}
            </span>
          </Text>
        </div>
        {channel.pricePerPostNano != null && (
          <div style={{ textAlign: 'right', flexShrink: 0 }}>
            <Text type="callout" weight="bold">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(channel.pricePerPostNano)}</span>
            </Text>
          </div>
        )}
      </div>

      {/* Row 2: stats + category */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 12, gap: 8 }}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexShrink: 0 }}>
          <Text type="caption1" color="secondary">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatSubscribers(channel.subscriberCount)}</span>
          </Text>
        </div>
        <div style={{ display: 'flex', gap: 8, overflow: 'hidden', flexShrink: 1, justifyContent: 'flex-end' }}>
          {topicName && <TopicBadge name={topicName} />}
        </div>
      </div>
    </motion.div>
  );
}
