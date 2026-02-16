import { Text } from '@telegram-tools/ui-kit';
import { useIsConnectionRestored } from '@tonconnect/ui-react';
import { ArrowUp, Plus, RefreshCw, SendHorizontal } from 'lucide-react';
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
import { AppPageShell, EmptyState, Tappable } from '@/shared/ui';
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
          icon={<ScrollIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
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
  const quickActions = [
    { key: 'transfer', label: t('wallet.quickAction.transfer'), Icon: SendHorizontal },
    { key: 'topUp', label: t('wallet.quickAction.topUp'), Icon: Plus },
    { key: 'withdraw', label: t('wallet.quickAction.withdraw'), Icon: ArrowUp },
    { key: 'exchange', label: t('wallet.quickAction.exchange'), Icon: RefreshCw },
  ] as const;

  return (
    <AppPageShell variant="finance" testId="wallet-page-shell">
      <motion.div {...fadeIn}>
        <div className="am-wallet-top">
          <div className="am-wallet-pill">
            <Text type="subheadline2" weight="medium">
              {t('wallet.title')}
            </Text>
          </div>

          <div className="am-wallet-segment">
            <div className="am-wallet-segment-active py-[7px] px-[18px]">
              <Text type="subheadline2" weight="bold">
                {t('wallet.segment.crypto')}
              </Text>
            </div>
            <div className="py-[7px] px-[18px]">
              <Text type="subheadline2" weight="bold">
                {t('wallet.segment.ton')}
              </Text>
            </div>
          </div>
        </div>

        <BalanceCard summary={summary} isOwner={isOwner} isConnectionRestored={isConnectionRestored} />

        <div className="am-wallet-actions">
          {quickActions.map(({ key, label, Icon }) => (
            <Tappable
              key={key}
              className="am-wallet-action"
              onClick={() => {
                haptic.selectionChanged();
              }}
              aria-label={label}
            >
              <div className="flex-center flex-col gap-2">
                <div className="am-wallet-actionIcon flex-center">
                  <Icon size={19} />
                </div>
                <Text type="caption1" weight="medium">
                  {label}
                </Text>
              </div>
            </Tappable>
          ))}
        </div>

        <motion.div {...slideUp} transition={{ delay: 0.2 }}>
          <MetricRow escrowAmount={escrowAmount} completedDealsCount={summary.completedDealsCount} />
        </motion.div>

        {transactions.length > 0 && (
          <motion.div {...fadeIn} transition={{ delay: 0.3 }}>
            <div className="flex justify-between items-center mb-2.5">
              <Text type="title3" weight="bold">
                {t('wallet.recentTransactions')}
              </Text>
              <motion.div {...pressScale} onClick={() => navigate('/wallet/history')} className="cursor-pointer">
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
