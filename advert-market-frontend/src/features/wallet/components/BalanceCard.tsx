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
  const heroLabel = isOwner ? t('wallet.hero.earned') : t('wallet.hero.spent');
  const gradientVar = isOwner ? 'var(--am-hero-gradient-success)' : 'var(--am-hero-gradient-accent)';

  return (
    <div
      className="am-finance-card"
      style={{
        overflow: 'hidden',
        position: 'relative',
      }}
    >
      {/* Gradient backdrop */}
      <div
        data-testid="balance-gradient"
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: 80,
          background: gradientVar,
          pointerEvents: 'none',
        }}
      />

      {/* Header row: label + TON Connect */}
      <div
        style={{
          position: 'relative',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '14px 16px 0',
        }}
      >
        <Text type="caption1" weight="bold" color="secondary">
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
      <div style={{ position: 'relative', textAlign: 'center', padding: '18px 16px 20px' }}>
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1, duration: 0.35, ease: easeOut }}
        >
          <Text type="largeTitle" weight="bold">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(heroAmount)}</span>
          </Text>
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.25, duration: 0.3 }}
          style={{ marginTop: 4 }}
        >
          <Text type="subheadline2" color="tertiary">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(heroAmount)}</span>
          </Text>
        </motion.div>
      </div>
    </div>
  );
}
