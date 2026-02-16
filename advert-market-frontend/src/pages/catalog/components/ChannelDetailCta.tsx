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
    <FixedBottomBar className="am-fixed-bottom-bar flex items-center gap-3">
      {minPrice != null && (
        <div className="shrink-0">
          <Text type="callout" weight="bold">
            <span className="am-tabnum">
              {t('catalog.channel.from', { price: formatTon(minPrice) })}
            </span>
          </Text>
          <Text type="caption1" color="tertiary">
            <span className="am-tabnum">{formatFiat(minPrice)}</span>
          </Text>
        </div>
      )}
      <div className="flex-1">
        <motion.div {...pressScale}>
          <Button
            text={t('catalog.channel.createDeal')}
            type="primary"
            onClick={() => {
              haptic.impactOccurred('medium');
              navigate(`/deals/new?channelId=${channelId}`);
            }}
          />
        </motion.div>
      </div>
    </FixedBottomBar>
  );
}
