import { Text } from '@telegram-tools/ui-kit';
import { easeOut } from 'motion';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { formatFiat } from '@/shared/lib/fiat-format';
import { formatTon } from '@/shared/lib/ton-format';
import type { WalletSummary } from '../types/wallet';

type SummaryHeroProps = {
  summary: WalletSummary;
};

function isOwnerView(summary: WalletSummary): boolean {
  return summary.earnedTotalNano !== '0';
}

export function SummaryHero({ summary }: SummaryHeroProps) {
  const { t } = useTranslation();
  const isOwner = isOwnerView(summary);

  const heroAmount = isOwner ? summary.earnedTotalNano : summary.spentTotalNano;
  const heroLabel = isOwner ? t('wallet.hero.earned') : t('wallet.hero.spent');

  return (
    <div style={{ position: 'relative', overflow: 'hidden' }}>
      {/* Gradient backdrop */}
      <div
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: 140,
          background: isOwner
            ? 'linear-gradient(180deg, rgba(52, 199, 89, 0.06) 0%, transparent 100%)'
            : 'linear-gradient(180deg, rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.06) 0%, transparent 100%)',
          pointerEvents: 'none',
        }}
      />

      <div
        style={{
          position: 'relative',
          textAlign: 'center',
          padding: '28px 16px 20px',
        }}
      >
        <Text type="caption1" color="secondary" weight="bold">
          {heroLabel}
        </Text>
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1, duration: 0.35, ease: easeOut }}
          style={{ marginTop: 6 }}
        >
          <Text type="hero" weight="bold">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(heroAmount)}</span>
          </Text>
        </motion.div>
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.25, duration: 0.3 }}
          style={{ marginTop: 4 }}
        >
          <Text type="subheadline2" color="secondary">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(heroAmount)}</span>
          </Text>
        </motion.div>
      </div>
    </div>
  );
}
