import { Text } from '@telegram-tools/ui-kit';
import { useIsConnectionRestored } from '@tonconnect/ui-react';
import { motion } from 'motion/react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { BalanceCard } from '@/features/wallet/components/BalanceCard';
import { MetricRow } from '@/features/wallet/components/MetricRow';
import { TransactionGroupList } from '@/features/wallet/components/TransactionGroupList';
import { WalletSkeleton } from '@/features/wallet/components/WalletSkeleton';
import { useTransactions } from '@/features/wallet/hooks/useTransactions';
import { useWalletSummary } from '@/features/wallet/hooks/useWalletSummary';
import { useHaptic } from '@/shared/hooks';
import { AppPageShell, EmptyState } from '@/shared/ui';
import { fadeIn, pressScale, slideUp } from '@/shared/ui/animations';
import { ScrollIcon } from '@/shared/ui/icons';

function isOwnerView(summary: { earnedTotalNano: string }): boolean {
  return summary.earnedTotalNano !== '0';
}

export default function WalletPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isConnectionRestored = useIsConnectionRestored();
  const haptic = useHaptic();

  const { data: summary, isLoading: summaryLoading } = useWalletSummary();
  const { data: txData, isLoading: txLoading } = useTransactions(undefined, 5);

  const transactions = useMemo(() => txData?.pages.flatMap((p) => p.items) ?? [], [txData]);

  const isLoading = summaryLoading || txLoading;
  const hasData =
    summary && (summary.earnedTotalNano !== '0' || summary.spentTotalNano !== '0' || transactions.length > 0);

  if (isLoading) {
    return <WalletSkeleton />;
  }

  if (!hasData) {
    return (
      <AppPageShell variant="finance" testId="wallet-page-shell">
        <EmptyState
          icon={<ScrollIcon className="w-7 h-7 text-fg-tertiary" />}
          title={t('wallet.empty.title')}
          description={t('wallet.empty.description')}
          actionLabel={t('wallet.empty.cta')}
          onAction={() => navigate('/catalog')}
        />
      </AppPageShell>
    );
  }

  const isOwner = isOwnerView(summary);
  const escrowAmount = isOwner ? summary.inEscrowNano : summary.activeEscrowNano;

  return (
    <AppPageShell variant="finance" testId="wallet-page-shell">
      <motion.div {...fadeIn} className="flex flex-col gap-5">
        <BalanceCard summary={summary} isOwner={isOwner} isConnectionRestored={isConnectionRestored} />

        <motion.div {...slideUp} transition={{ delay: 0.2 }}>
          <MetricRow escrowAmount={escrowAmount} completedDealsCount={summary.completedDealsCount} />
        </motion.div>

        {transactions.length > 0 && (
          <motion.div {...fadeIn} transition={{ delay: 0.3 }}>
            <div className="flex justify-between items-center mb-4">
              <Text type="title3" weight="bold">
                {t('wallet.recentTransactions')}
              </Text>
              <motion.div
                {...pressScale}
                onClick={() => {
                  haptic.impactOccurred('light');
                  navigate('/wallet/history');
                }}
                className="cursor-pointer"
              >
                <Text type="subheadline2" weight="medium" color="accent">
                  {t('wallet.viewAll')}
                </Text>
              </motion.div>
            </div>
            <TransactionGroupList
              transactions={transactions}
              onItemClick={(txId) => navigate(`/wallet/history/${txId}`)}
            />
          </motion.div>
        )}
      </motion.div>
    </AppPageShell>
  );
}
