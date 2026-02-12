import { GroupItem, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { formatTon } from '@/shared/lib/ton-format';
import { listItem, pressScale } from '@/shared/ui/animations';
import type { Channel } from '../types/channel';

type ChannelListItemProps = {
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
  );
}

export function ChannelListItem({ channel, onClick }: ChannelListItemProps) {
  const { t } = useTranslation();

  return (
    <motion.div {...listItem} {...pressScale}>
      <GroupItem
        text={channel.title}
        description={`${formatSubscribers(channel.subscriberCount)} ${t('catalog.channel.subscribers', { count: '' }).trim()}`}
        before={<ChannelAvatar title={channel.title} />}
        after={
          channel.pricePerPostNano ? (
            <Text type="callout" weight="medium">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(channel.pricePerPostNano)}</span>
            </Text>
          ) : null
        }
        chevron
        onClick={onClick}
      />
    </motion.div>
  );
}
