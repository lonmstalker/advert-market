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

  const isAvailable = !timeUntil;

  return (
    <motion.div {...slideUp} className="px-4 pt-4">
      <div
        className={`flex items-center gap-2 py-2.5 px-3.5 rounded-[10px] border ${
          isAvailable ? 'bg-soft-success border-success/20' : 'bg-bg-secondary border-separator'
        }`}
      >
        <ClockIcon className={`w-4 h-4 shrink-0 ${isAvailable ? 'text-success' : 'text-fg-secondary'}`} />
        <Text type="caption1" color={isAvailable ? undefined : 'secondary'}>
          <span className={isAvailable ? 'text-success font-medium' : ''}>
            {timeUntil ? t('catalog.channel.nextSlot', { time: timeUntil }) : t('catalog.channel.nextSlotAvailable')}
          </span>
        </Text>
      </div>
    </motion.div>
  );
}
