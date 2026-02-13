import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { formatRelativeTime } from '@/shared/lib/date-format';
import { formatFiat } from '@/shared/lib/fiat-format';
import { formatTon } from '@/shared/lib/ton-format';
import { listItem, pressScale } from '@/shared/ui/animations';
import type { DealListItem as DealListItemType } from '../types/deal';
import { DealStatusBadge } from './DealStatusBadge';

type DealListItemProps = {
  deal: DealListItemType;
  onClick: () => void;
};

function DealAvatar({ title }: { title: string }) {
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

export function DealListItem({ deal, onClick }: DealListItemProps) {
  const { t, i18n } = useTranslation();

  const freq = deal.postFrequencyHours;
  const dur = deal.durationHours;
  const overlapLabel =
    freq && dur
      ? t('catalog.channel.overlapFormat', { freq, dur })
      : dur
        ? t('catalog.channel.onlyDuration', { dur })
        : freq
          ? t('catalog.channel.onlyFrequency', { freq })
          : null;

  return (
    <motion.div {...listItem} {...pressScale} onClick={onClick} style={{ cursor: 'pointer' }}>
      <div
        style={{
          background: 'var(--color-background-base)',
          border: '1px solid var(--color-border-separator)',
          borderRadius: 16,
          padding: '14px 16px',
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
        }}
      >
        {/* Row 1: avatar + title + price */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <DealAvatar title={deal.channelTitle} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <Text type="body" weight="medium">
              <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {deal.channelTitle}
              </span>
            </Text>
            {overlapLabel && (
              <Text type="caption1" color="tertiary">
                {overlapLabel}
              </Text>
            )}
          </div>
          <div style={{ flexShrink: 0, textAlign: 'right' }}>
            <Text type="callout" weight="bold">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(deal.priceNano)}</span>
            </Text>
            <Text type="caption1" color="tertiary">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(deal.priceNano)}</span>
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
}
