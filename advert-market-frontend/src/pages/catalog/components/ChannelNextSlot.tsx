import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { formatTimeUntil } from '@/shared/lib/time-utils';
import { slideUp } from '@/shared/ui/animations';
import { ClockIcon } from '@/shared/ui/icons';

type ChannelNextSlotProps = {
  nextAvailableSlot: string;
};

export function ChannelNextSlot({ nextAvailableSlot }: ChannelNextSlotProps) {
  const { t } = useTranslation();
  const timeUntil = formatTimeUntil(nextAvailableSlot);

  return (
    <motion.div {...slideUp} style={{ padding: '12px 16px 0' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          padding: '10px 14px',
          borderRadius: 10,
          background: timeUntil
            ? 'var(--color-background-section)'
            : 'color-mix(in srgb, var(--color-state-success) 8%, transparent)',
          border: timeUntil
            ? '1px solid var(--color-border-separator)'
            : '1px solid color-mix(in srgb, var(--color-state-success) 20%, transparent)',
        }}
      >
        <ClockIcon
          style={{
            width: 16,
            height: 16,
            color: timeUntil ? 'var(--color-foreground-secondary)' : 'var(--color-state-success)',
            flexShrink: 0,
          }}
        />
        <Text type="caption1" color={timeUntil ? 'secondary' : undefined}>
          <span style={{ color: timeUntil ? undefined : 'var(--color-state-success)', fontWeight: 500 }}>
            {timeUntil ? t('catalog.channel.nextSlot', { time: timeUntil }) : t('catalog.channel.nextSlotAvailable')}
          </span>
        </Text>
      </div>
    </motion.div>
  );
}
