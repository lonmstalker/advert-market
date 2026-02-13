import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { pressScale, slideUp } from '@/shared/ui/animations';
import { ArrowRightIcon, TelegramIcon } from '@/shared/ui/icons';

type ChannelOpenTelegramProps = {
  link: string;
  username?: string;
};

export function ChannelOpenTelegram({ link, username }: ChannelOpenTelegramProps) {
  const { t } = useTranslation();

  return (
    <motion.div {...slideUp} style={{ padding: '0 16px 8px' }}>
      <motion.button
        {...pressScale}
        type="button"
        onClick={() => window.open(link, '_blank')}
        style={{
          width: '100%',
          background: 'var(--color-background-base)',
          border: '1px solid var(--color-border-separator)',
          borderRadius: 12,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '14px 16px',
          WebkitTapHighlightColor: 'transparent',
        }}
      >
        <div
          style={{
            width: 36,
            height: 36,
            borderRadius: '50%',
            background: 'color-mix(in srgb, var(--color-link) 8%, transparent)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <TelegramIcon style={{ width: 18, height: 18, color: 'var(--color-link)' }} />
        </div>
        <div style={{ flex: 1, minWidth: 0, textAlign: 'left' }}>
          <span style={{ display: 'block', fontSize: 14, fontWeight: 500, color: 'var(--color-link)' }}>
            {username ? t('catalog.channel.openInTelegram') : t('catalog.channel.joinChannel')}
          </span>
          {username && (
            <span style={{ display: 'block', fontSize: 12, color: 'var(--color-foreground-tertiary)', marginTop: 2 }}>
              @{username}
            </span>
          )}
        </div>
        <ArrowRightIcon style={{ width: 16, height: 16, color: 'var(--color-link)', opacity: 0.5, flexShrink: 0 }} />
      </motion.button>
    </motion.div>
  );
}
