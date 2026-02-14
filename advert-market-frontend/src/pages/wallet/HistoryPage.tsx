import { Spinner, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { TransactionFilterSheet } from '@/features/wallet/components/TransactionFilterSheet';
import { TransactionGroupList } from '@/features/wallet/components/TransactionGroupList';
import { useTransactions } from '@/features/wallet/hooks/useTransactions';
import type { TransactionFilters } from '@/features/wallet/types/wallet';
import { useInfiniteScroll } from '@/shared/hooks/use-infinite-scroll';
import { BackButtonHandler, EmptyState, EndOfList } from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';

const EMPTY_FILTERS: TransactionFilters = {};

export default function HistoryPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [filters, setFilters] = useState<TransactionFilters>(EMPTY_FILTERS);
  const [sheetOpen, setSheetOpen] = useState(false);

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useTransactions(filters);

  const transactions = useMemo(() => data?.pages.flatMap((p) => p.items) ?? [], [data]);

  const sentinelRef = useInfiniteScroll({ hasNextPage, isFetchingNextPage, fetchNextPage });

  const activeFilterCount = [filters.type, filters.from, filters.to].filter(Boolean).length;

  return (
    <>
      <BackButtonHandler />
      <motion.div {...fadeIn} style={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
        <div style={{ padding: '16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Text type="title1" weight="bold">
            {t('wallet.history.title')}
          </Text>
          <motion.button
            {...pressScale}
            type="button"
            onClick={() => setSheetOpen(true)}
            style={{
              background: activeFilterCount > 0 ? 'var(--color-accent-primary)' : 'var(--color-background-secondary)',
              border: 'none',
              borderRadius: 8,
              padding: '6px 12px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 4,
            }}
          >
            <Text type="caption1" weight="bold" color={activeFilterCount > 0 ? 'white' : 'primary'}>
              {t('wallet.filters.button')}
              {activeFilterCount > 0 ? ` (${activeFilterCount})` : ''}
            </Text>
          </motion.button>
        </div>

        {isLoading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
            <Spinner size="32px" color="accent" />
          </div>
        ) : transactions.length === 0 ? (
          <EmptyState
            emoji="📜"
            title={t('wallet.history.empty')}
            description={activeFilterCount > 0 ? t('wallet.history.emptyFiltered') : t('wallet.empty.description')}
            actionLabel={activeFilterCount > 0 ? t('wallet.filters.reset') : undefined}
            onAction={activeFilterCount > 0 ? () => setFilters(EMPTY_FILTERS) : undefined}
          />
        ) : (
          <div style={{ padding: '0 16px' }}>
            <TransactionGroupList
              transactions={transactions}
              onItemClick={(txId) => navigate(`/wallet/history/${txId}`)}
            />

            <div ref={sentinelRef} style={{ height: 1 }} />

            {isFetchingNextPage && (
              <div style={{ display: 'flex', justifyContent: 'center', padding: 16 }}>
                <Spinner size="24px" color="accent" />
              </div>
            )}

            {!hasNextPage && transactions.length > 0 && <EndOfList label={t('wallet.history.endOfList')} />}
          </div>
        )}
      </motion.div>

      <TransactionFilterSheet
        open={sheetOpen}
        onClose={() => setSheetOpen(false)}
        filters={filters}
        onApply={setFilters}
        onReset={() => {
          setFilters(EMPTY_FILTERS);
          setSheetOpen(false);
        }}
      />
    </>
  );
}
