import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { memo } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { formatRelativeTime } from '@/shared/lib/date-format';
import { formatTon } from '@/shared/lib/ton-format';
import { listItem, pressScale } from '@/shared/ui/animations';
import { ChannelAvatar } from '@/shared/ui/components/channel-avatar';
import type { DealListItem as DealListItemType } from '../types/deal';
import { DealStatusBadge } from './DealStatusBadge';

type DealListItemProps = {
  deal: DealListItemType;
  onClick: () => void;
};

export const DealListItem = memo(function DealListItem({ deal, onClick }: DealListItemProps) {
  const { i18n } = useTranslation();
  const haptic = useHaptic();

  return (
    <motion.div
      {...listItem}
      {...pressScale}
      onClick={() => {
        haptic.impactOccurred('light');
        onClick();
      }}
      style={{ cursor: 'pointer' }}
    >
      <div
        style={{
          background: 'var(--color-background-base)',
          border: '1px solid var(--color-border-separator)',
          borderRadius: 14,
          padding: '14px 16px',
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
        }}
      >
        {/* Row 1: avatar + title + price */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <ChannelAvatar title={deal.channelTitle} size="sm" />
          <div style={{ flex: 1, minWidth: 0 }}>
            <Text type="body" weight="medium">
              <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {deal.channelTitle}
              </span>
            </Text>
          </div>
          <div style={{ flexShrink: 0 }}>
            <Text type="callout" weight="bold">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(deal.priceNano)}</span>
            </Text>
          </div>
        </div>

        {/* Row 2: status badge + relative time */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <DealStatusBadge status={deal.status} />
          <Text type="caption1" color="secondary">
            {formatRelativeTime(deal.updatedAt, i18n.language)}
          </Text>
        </div>
      </div>
    </motion.div>
  );
});
