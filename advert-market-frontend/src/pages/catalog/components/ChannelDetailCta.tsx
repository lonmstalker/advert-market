import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { formatFiat } from '@/shared/lib/fiat-format';
import { formatTon } from '@/shared/lib/ton-format';
import { pressScale } from '@/shared/ui/animations';

type ChannelDetailCtaProps = {
  channelId: number;
  minPrice: number | null;
};

export function ChannelDetailCta({ channelId, minPrice }: ChannelDetailCtaProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        padding: '10px 16px',
        background: 'var(--color-background-base)',
        borderTop: '1px solid var(--color-border-separator)',
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        zIndex: 10,
      }}
    >
      {minPrice != null && (
        <div style={{ flexShrink: 0 }}>
          <Text type="callout" weight="bold">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>
              {t('catalog.channel.from', { price: formatTon(minPrice) })}
            </span>
          </Text>
          <Text type="caption1" color="tertiary">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(minPrice)}</span>
          </Text>
        </div>
      )}
      <div style={{ flex: 1 }}>
        <motion.button
          {...pressScale}
          type="button"
          onClick={() => navigate(`/deals/new?channelId=${channelId}`)}
          style={{
            width: '100%',
            padding: '10px 16px',
            background: 'var(--color-accent-primary)',
            border: 'none',
            borderRadius: 10,
            cursor: 'pointer',
            fontSize: 14,
            fontWeight: 600,
            color: 'var(--color-static-white)',
            WebkitTapHighlightColor: 'transparent',
          }}
        >
          {t('catalog.channel.createDeal')}
        </motion.button>
      </div>
    </div>
  );
}
