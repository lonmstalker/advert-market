import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { slideUp } from '@/shared/ui/animations';

type ChannelOwnerNoteProps = {
  customRules: string;
};

export function ChannelOwnerNote({ customRules }: ChannelOwnerNoteProps) {
  const { t } = useTranslation();

  return (
    <motion.div {...slideUp} className="px-4 pt-3 pb-4">
      <div className="p-3.5 rounded-xl bg-[color-mix(in_srgb,var(--color-accent-primary)_5%,var(--color-background-base))] border border-[color-mix(in_srgb,var(--color-accent-primary)_12%,transparent)]">
        <Text type="caption2" weight="bold" color="accent">
          <span className="block uppercase tracking-wider mb-2">{t('catalog.channel.ownerNote')}</span>
        </Text>
        <div className="whitespace-pre-wrap">
          <Text type="subheadline1" color="secondary">
            {customRules}
          </Text>
        </div>
      </div>
    </motion.div>
  );
}
