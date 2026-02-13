import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import type { ChannelDetail } from '@/features/channels';
import { engagementRateColor } from '@/shared/lib/engagement-rate';
import { formatCompactNumber, formatLocaleNumber } from '@/shared/lib/format-number';
import { slideUp } from '@/shared/ui/animations';

type ChannelDetailStatsProps = {
  channel: ChannelDetail;
};

export function ChannelDetailStats({ channel }: ChannelDetailStatsProps) {
  const { t } = useTranslation();

  const reachRate =
    channel.avgReach != null && channel.subscriberCount > 0 ? (channel.avgReach / channel.subscriberCount) * 100 : null;

  return (
    <motion.div {...slideUp} style={{ padding: '16px 16px 0', display: 'flex', gap: 8 }}>
      <div style={statCardStyle}>
        <Text type="title2" weight="bold">
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatLocaleNumber(channel.subscriberCount)}</span>
        </Text>
        <Text type="caption1" color="secondary">
          {t('catalog.channel.subscribersStat')}
        </Text>
      </div>
      {channel.avgReach != null && (
        <div style={statCardStyle}>
          <Text type="title2" weight="bold">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatCompactNumber(channel.avgReach)}</span>
          </Text>
          <Text type="caption1" color="secondary">
            {t('catalog.channel.avgReach')}
          </Text>
          {reachRate != null && (
            <Text type="caption1" color="tertiary">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{reachRate.toFixed(0)}% reach</span>
            </Text>
          )}
        </div>
      )}
      {channel.engagementRate != null && (
        <div style={statCardStyle}>
          <Text type="title2" weight="bold">
            <span style={{ fontVariantNumeric: 'tabular-nums', color: engagementRateColor(channel.engagementRate) }}>
              {channel.engagementRate.toFixed(1)}%
            </span>
          </Text>
          <Text type="caption1" color="secondary">
            {t('catalog.channel.er')}
          </Text>
        </div>
      )}
    </motion.div>
  );
}

const statCardStyle: React.CSSProperties = {
  flex: 1,
  background: 'var(--color-background-base)',
  border: '1px solid var(--color-border-separator)',
  borderRadius: 12,
  padding: 12,
  textAlign: 'center',
};
