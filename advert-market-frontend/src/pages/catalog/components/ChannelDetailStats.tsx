import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import type { ReactNode } from 'react';
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

  const cells: ReactNode[] = [];

  cells.push(
    <div key="subs" style={statCellStyle}>
      <Text type="title2" weight="bold">
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatLocaleNumber(channel.subscriberCount)}</span>
      </Text>
      <Text type="caption1" color="secondary">
        {t('catalog.channel.subscribersStat')}
      </Text>
    </div>,
  );

  if (channel.avgReach != null) {
    cells.push(
      <div key="reach" style={statCellStyle}>
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
      </div>,
    );
  }

  if (channel.engagementRate != null) {
    cells.push(
      <div key="er" style={statCellStyle}>
        <Text type="title2" weight="bold">
          <span style={{ fontVariantNumeric: 'tabular-nums', color: engagementRateColor(channel.engagementRate) }}>
            {channel.engagementRate.toFixed(1)}%
          </span>
        </Text>
        <Text type="caption1" color="secondary">
          {t('catalog.channel.er')}
        </Text>
      </div>,
    );
  }

  const items: ReactNode[] = [];
  for (let i = 0; i < cells.length; i++) {
    if (i > 0) {
      items.push(<div key={`div-${i}`} style={dividerStyle} />);
    }
    items.push(cells[i]);
  }

  return (
    <motion.div {...slideUp} style={{ padding: '16px 16px 0' }}>
      <div style={statsContainerStyle}>{items}</div>
    </motion.div>
  );
}

const statsContainerStyle: React.CSSProperties = {
  display: 'flex',
  background: 'var(--color-background-base)',
  border: '1px solid var(--color-border-separator)',
  borderRadius: 14,
  overflow: 'hidden',
};

const statCellStyle: React.CSSProperties = {
  flex: 1,
  padding: 12,
  textAlign: 'center',
};

const dividerStyle: React.CSSProperties = {
  width: 1,
  alignSelf: 'stretch',
  background: 'var(--color-border-separator)',
};
