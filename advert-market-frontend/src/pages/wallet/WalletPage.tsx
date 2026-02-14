import { Spinner, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { QuickActions } from '@/features/wallet/components/QuickActions';
import { SummaryHero } from '@/features/wallet/components/SummaryHero';
import { SummaryStats } from '@/features/wallet/components/SummaryStats';
import { TransactionGroupList } from '@/features/wallet/components/TransactionGroupList';
import { useTransactions } from '@/features/wallet/hooks/useTransactions';
import { useWalletSummary } from '@/features/wallet/hooks/useWalletSummary';
import { EmptyState } from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';

export default function WalletPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const { data: summary, isLoading: summaryLoading } = useWalletSummary();
  const { data: txData, isLoading: txLoading } = useTransactions(undefined, 5);

  const transactions = useMemo(() => txData?.pages.flatMap((p) => p.items) ?? [], [txData]);

  const isLoading = summaryLoading || txLoading;
  const hasData =
    summary && (summary.earnedTotalNano !== '0' || summary.spentTotalNano !== '0' || transactions.length > 0);

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spinner size="32px" color="accent" />
      </div>
    );
  }

  if (!hasData) {
    return (
      <div style={{ padding: '16px' }}>
        <Text type="title1" weight="bold">
          {t('wallet.title')}
        </Text>
        <EmptyState
          emoji="📜"
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

      <SummaryHero summary={summary} />

      <div style={{ padding: '4px 16px 16px' }}>
        <QuickActions />
      </div>

      <div style={{ padding: '0 16px 16px' }}>
        <SummaryStats summary={summary} />
      </div>

      {transactions.length > 0 && (
        <div style={{ padding: '0 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <Text type="title3" weight="bold">
              {t('wallet.recentTransactions')}
            </Text>
            <motion.a
              {...pressScale}
              href="/wallet/history"
              onClick={(e) => {
                e.preventDefault();
                navigate('/wallet/history');
              }}
              style={{
                color: 'var(--color-accent-primary)',
                fontSize: 14,
                fontWeight: 500,
                textDecoration: 'none',
                cursor: 'pointer',
              }}
            >
              {t('wallet.viewAll')}
            </motion.a>
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
