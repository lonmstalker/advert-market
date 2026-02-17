import { useMutation } from '@tanstack/react-query';
import { Text } from '@telegram-tools/ui-kit';
import { useIsConnectionRestored } from '@tonconnect/ui-react';
import { motion } from 'motion/react';
import { useEffect, useMemo, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { updateWallet } from '@/features/profile';
import { BalanceCard } from '@/features/wallet/components/BalanceCard';
import { MetricRow } from '@/features/wallet/components/MetricRow';
import { TransactionGroupList } from '@/features/wallet/components/TransactionGroupList';
import { WalletSkeleton } from '@/features/wallet/components/WalletSkeleton';
import { useTransactions } from '@/features/wallet/hooks/useTransactions';
import { useWalletSummary } from '@/features/wallet/hooks/useWalletSummary';
import { useHaptic } from '@/shared/hooks';
import { useTonWalletStatus } from '@/shared/ton';
import { AppPageShell, EmptyState } from '@/shared/ui';
import { fadeIn, pressScale, slideUp } from '@/shared/ui/animations';
import { ScrollIcon } from '@/shared/ui/icons';

function isOwnerView(summary: { earnedTotalNano: string }): boolean {
  return summary.earnedTotalNano !== '0';
}

const EMPTY_SUMMARY = {
  earnedTotalNano: '0',
  inEscrowNano: '0',
  spentTotalNano: '0',
  activeEscrowNano: '0',
  activeDealsCount: 0,
  completedDealsCount: 0,
} as const;

export default function WalletPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isConnectionRestored = useIsConnectionRestored();
  const haptic = useHaptic();
  const { isConnected, friendlyAddress } = useTonWalletStatus();
  const syncedAddressRef = useRef<string | null>(null);

  const { data: summary, isLoading: summaryLoading } = useWalletSummary();
  const { data: txData, isLoading: txLoading } = useTransactions(undefined, 5);
  const syncWalletMutation = useMutation({
    mutationFn: updateWallet,
  });

  const transactions = useMemo(() => txData?.pages.flatMap((p) => p.items) ?? [], [txData]);

  const isLoading = summaryLoading || txLoading;
  const resolvedSummary = summary ?? EMPTY_SUMMARY;

  useEffect(() => {
    const address = friendlyAddress?.trim();
    if (!isConnected || !address || syncedAddressRef.current === address) {
      return;
    }
    syncedAddressRef.current = address;
    syncWalletMutation.mutate(address);
  }, [friendlyAddress, isConnected, syncWalletMutation]);

  if (isLoading) {
    return <WalletSkeleton />;
  }
  const isOwner = isOwnerView(resolvedSummary);
  const escrowAmount = isOwner ? resolvedSummary.inEscrowNano : resolvedSummary.activeEscrowNano;

  return (
    <AppPageShell variant="finance" testId="wallet-page-shell">
      <motion.div {...fadeIn} className="flex flex-col gap-5">
        <BalanceCard summary={resolvedSummary} isOwner={isOwner} isConnectionRestored={isConnectionRestored} />

        <motion.div {...slideUp} transition={{ delay: 0.2 }}>
          <MetricRow escrowAmount={escrowAmount} completedDealsCount={resolvedSummary.completedDealsCount} />
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

        {transactions.length === 0 && (
          <motion.div {...fadeIn} transition={{ delay: 0.3 }}>
            <EmptyState
              icon={<ScrollIcon className="w-7 h-7 text-fg-tertiary" />}
              title={t('wallet.empty.title')}
              description={t('wallet.empty.description')}
              actionLabel={t('wallet.empty.cta')}
              onAction={() => navigate('/catalog')}
            />
          </motion.div>
        )}
      </motion.div>
    </AppPageShell>
  );
}
