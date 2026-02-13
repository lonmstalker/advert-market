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
    <motion.div {...slideUp} style={{ padding: '0 16px 8px' }}>
      <div
        style={{
          padding: '14px 16px',
          borderRadius: 12,
          background: 'color-mix(in srgb, var(--color-accent-primary) 5%, var(--color-background-base))',
          border: '1px solid color-mix(in srgb, var(--color-accent-primary) 12%, transparent)',
        }}
      >
        <span
          style={{
            display: 'block',
            fontSize: 11,
            fontWeight: 700,
            color: 'var(--color-accent-primary)',
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
            marginBottom: 8,
          }}
        >
          {t('catalog.channel.ownerNote')}
        </span>
        <Text type="subheadline1" color="secondary" style={{ whiteSpace: 'pre-wrap' }}>
          {customRules}
        </Text>
      </div>
    </motion.div>
  );
}
