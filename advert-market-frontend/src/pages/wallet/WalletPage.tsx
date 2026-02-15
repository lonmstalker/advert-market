import { Spinner, Text } from '@telegram-tools/ui-kit';
import { TonConnectButton, useIsConnectionRestored } from '@tonconnect/ui-react';
import { motion } from 'motion/react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { SummaryHero } from '@/features/wallet/components/SummaryHero';
import { SummaryStats } from '@/features/wallet/components/SummaryStats';
import { TransactionGroupList } from '@/features/wallet/components/TransactionGroupList';
import { WalletSkeleton } from '@/features/wallet/components/WalletSkeleton';
import { useTransactions } from '@/features/wallet/hooks/useTransactions';
import { useWalletSummary } from '@/features/wallet/hooks/useWalletSummary';
import { EmptyState } from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';
import { ScrollIcon } from '@/shared/ui/icons';

export default function WalletPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const isConnectionRestored = useIsConnectionRestored();

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
      <div style={{ padding: '16px' }}>
        <Text type="title1" weight="bold">
          {t('wallet.title')}
        </Text>
        <EmptyState
          icon={<ScrollIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
          title={t('wallet.empty.title')}
          description={t('wallet.empty.description')}
          actionLabel={t('wallet.empty.cta')}
          onAction={() => navigate('/catalog')}
        />
      </div>
    );
  }

  return (
    <motion.div {...fadeIn} style={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      <div style={{ padding: '16px 16px 0' }}>
        <Text type="title1" weight="bold">
          {t('wallet.title')}
        </Text>
      </div>

      <div style={{ padding: '12px 16px 4px' }}>
        <div
          style={{
            padding: '12px 12px',
            borderRadius: 14,
            background: 'var(--color-background-secondary)',
            border: '1px solid var(--color-border-separator)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 12,
          }}
        >
          <div style={{ minWidth: 0 }}>
            <Text type="caption1" color="secondary">
              {t('wallet.connection.title')}
            </Text>
            <div style={{ marginTop: 2 }}>
              <Text type="subheadline2" color="secondary">
                {t('wallet.connection.hint')}
              </Text>
            </div>
          </div>
          {isConnectionRestored ? (
            <div data-testid="wallet-ton-connect">
              <TonConnectButton />
            </div>
          ) : (
            <Spinner size="20px" color="accent" />
          )}
        </div>
      </div>

      <SummaryHero summary={summary} />

      <div style={{ padding: '0 16px 16px' }}>
        <SummaryStats summary={summary} />
      </div>

      {transactions.length > 0 && (
        <div style={{ padding: '0 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <Text type="title3" weight="bold">
              {t('wallet.recentTransactions')}
            </Text>
            <motion.div {...pressScale} onClick={() => navigate('/wallet/history')} style={{ cursor: 'pointer' }}>
              <Text type="subheadline2" weight="medium" color="accent">
                {t('wallet.viewAll')}
              </Text>
            </motion.div>
          </div>
          <TransactionGroupList
            transactions={transactions}
            onItemClick={(txId) => navigate(`/wallet/history/${txId}`)}
          />
        </div>
      )}
    </motion.div>
  );
}
