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
    <div key="subs" className="flex-1 p-3 text-center">
      <Text type="title2" weight="bold">
        <span className="am-tabnum">{formatLocaleNumber(channel.subscriberCount)}</span>
      </Text>
      <Text type="caption1" color="secondary">
        {t('catalog.channel.subscribersStat')}
      </Text>
    </div>,
  );

  if (channel.avgReach != null) {
    cells.push(
      <div key="reach" className="flex-1 p-3 text-center">
        <Text type="title2" weight="bold">
          <span className="am-tabnum">{formatCompactNumber(channel.avgReach)}</span>
        </Text>
        <Text type="caption1" color="secondary">
          {t('catalog.channel.avgReach')}
        </Text>
        {reachRate != null && (
          <Text type="caption1" color="tertiary">
            <span className="am-tabnum">{reachRate.toFixed(0)}% reach</span>
          </Text>
        )}
      </div>,
    );
  }

  if (channel.engagementRate != null) {
    cells.push(
      <div key="er" className="flex-1 p-3 text-center">
        <Text type="title2" weight="bold">
          <span className="am-tabnum" style={{ color: engagementRateColor(channel.engagementRate) }}>
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
      items.push(<div key={`div-${i}`} className="w-px self-stretch bg-separator" />);
    }
    items.push(cells[i]);
  }

  return (
    <motion.div {...slideUp} className="px-4 pt-4 pb-3">
      <div className="flex bg-bg-base border border-separator rounded-[14px] overflow-hidden">{items}</div>
    </motion.div>
  );
}
