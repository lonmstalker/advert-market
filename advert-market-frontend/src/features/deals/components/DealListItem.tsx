import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { memo } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { formatRelativeTime } from '@/shared/lib/date-format';
import { formatTon } from '@/shared/lib/ton-format';
import { AppSurfaceCard } from '@/shared/ui';
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
      data-testid={`deal-list-item-${deal.id}`}
      onClick={() => {
        haptic.impactOccurred('light');
        onClick();
      }}
      className="cursor-pointer [-webkit-tap-highlight-color:transparent]"
    >
      <AppSurfaceCard>
        <div className="flex flex-col gap-2.5 px-4 py-4">
          <div className="flex items-center gap-3">
            <ChannelAvatar title={deal.channelTitle} size="sm" />
            <div className="flex-1 min-w-0">
              <Text type="body" weight="medium">
                <span className="block truncate">{deal.channelTitle}</span>
              </Text>
            </div>
            <div className="shrink-0">
              <Text type="callout" weight="bold">
                <span className="tabular-nums">{formatTon(deal.priceNano)}</span>
              </Text>
            </div>
          </div>

          <div className="flex items-center justify-between">
            <DealStatusBadge status={deal.status} />
            <Text type="caption1" color="secondary">
              {formatRelativeTime(deal.updatedAt, i18n.language)}
            </Text>
          </div>
        </div>
      </AppSurfaceCard>
    </motion.div>
  );
});
