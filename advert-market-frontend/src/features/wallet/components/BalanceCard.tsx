import { Spinner, Text } from '@telegram-tools/ui-kit';
import { TonConnectButton } from '@tonconnect/ui-react';
import { easeOut } from 'motion';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { formatFiat } from '@/shared/lib/fiat-format';
import { formatTon } from '@/shared/lib/ton-format';
import type { WalletSummary } from '../types/wallet';

type BalanceCardProps = {
  summary: WalletSummary;
  isOwner: boolean;
  isConnectionRestored: boolean;
};

export function BalanceCard({ summary, isOwner, isConnectionRestored }: BalanceCardProps) {
  const { t } = useTranslation();

  const heroAmount = isOwner ? summary.earnedTotalNano : summary.spentTotalNano;
  const contextAmount = isOwner ? summary.inEscrowNano : summary.activeEscrowNano;
  const heroLabel = isOwner ? t('wallet.hero.earned') : t('wallet.hero.spent');
  const gradientVar = isOwner ? 'var(--am-hero-gradient-success)' : 'var(--am-hero-gradient-accent)';

  return (
    <div className="am-finance-card overflow-hidden relative">
      {/* Gradient backdrop */}
      <div
        data-testid="balance-gradient"
        className="absolute inset-x-0 top-0 h-[120px] pointer-events-none"
        style={{ background: gradientVar }}
      />

      {/* Header row: label + TON Connect */}
      <div className="relative flex items-center justify-between pt-5 px-5">
        <Text type="subheadline2" weight="medium" color="secondary">
          {heroLabel}
        </Text>
        {isConnectionRestored ? (
          <div data-testid="wallet-ton-connect">
            <TonConnectButton />
          </div>
        ) : (
          <Spinner size="20px" color="accent" />
        )}
      </div>

      {/* Balance centered */}
      <div className="relative text-center pt-6 pb-7 px-5">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1, duration: 0.35, ease: easeOut }}
        >
          <Text type="hero" weight="bold">
            <span className="am-tabnum">{formatTon(heroAmount)}</span>
          </Text>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.25, duration: 0.3 }}
          className="mt-1.5"
        >
          <Text type="subheadline2" color="tertiary">
            <span className="am-tabnum">{formatFiat(heroAmount)}</span>
          </Text>
        </motion.div>

        <div className="mt-3.5">
          <div className="am-wallet-positiveChip">
            <Text type="caption1" weight="medium">
              {t('wallet.stats.inEscrow')}: {formatTon(contextAmount)}
            </Text>
          </div>
        </div>
      </div>
    </div>
  );
}
