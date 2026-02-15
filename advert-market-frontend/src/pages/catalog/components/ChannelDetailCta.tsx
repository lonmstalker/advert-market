import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { formatFiat } from '@/shared/lib/fiat-format';
import { formatTon } from '@/shared/lib/ton-format';
import { FixedBottomBar } from '@/shared/ui';
import { pressScale } from '@/shared/ui/animations';

type ChannelDetailCtaProps = {
  channelId: number;
  minPrice: number | null;
};

export function ChannelDetailCta({ channelId, minPrice }: ChannelDetailCtaProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const haptic = useHaptic();

  return (
    <FixedBottomBar style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
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
        <motion.div {...pressScale}>
          <Button
            text={t('catalog.channel.createDeal')}
            type="primary"
            onClick={() => {
              haptic.impactOccurred('light');
              navigate(`/deals/new?channelId=${channelId}`);
            }}
          />
        </motion.div>
      </div>
    </FixedBottomBar>
  );
}
