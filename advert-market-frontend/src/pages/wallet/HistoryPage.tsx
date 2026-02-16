import { Spinner, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { TransactionFilterSheet } from '@/features/wallet/components/TransactionFilterSheet';
import { TransactionGroupList } from '@/features/wallet/components/TransactionGroupList';
import { TransactionListSkeleton } from '@/features/wallet/components/TransactionListSkeleton';
import { useTransactions } from '@/features/wallet/hooks/useTransactions';
import type { TransactionFilters } from '@/features/wallet/types/wallet';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useInfiniteScroll } from '@/shared/hooks/use-infinite-scroll';
import { AppPageShell, BackButtonHandler, EmptyState, EndOfList, FilterButton } from '@/shared/ui';
import { fadeIn } from '@/shared/ui/animations';
import { ScrollIcon } from '@/shared/ui/icons';

const EMPTY_FILTERS: TransactionFilters = {};

export default function HistoryPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const haptic = useHaptic();
  const [filters, setFilters] = useState<TransactionFilters>(EMPTY_FILTERS);
  const [sheetOpen, setSheetOpen] = useState(false);

  const handleFilterClick = useCallback(() => {
    haptic.impactOccurred('light');
    setSheetOpen(true);
  }, [haptic]);

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useTransactions(filters);

  const transactions = useMemo(() => data?.pages.flatMap((p) => p.items) ?? [], [data]);

  const sentinelRef = useInfiniteScroll({ hasNextPage, isFetchingNextPage, fetchNextPage });

  const activeFilterCount = [filters.type, filters.from, filters.to].filter(Boolean).length;

  return (
    <>
      <BackButtonHandler />
      <AppPageShell variant="finance" testId="wallet-history-page-shell">
        <motion.div {...fadeIn}>
          <div className="am-history-header">
            <div className="flex items-center justify-between">
              <Text type="title1" weight="bold">
                {t('wallet.history.title')}
              </Text>
              <FilterButton activeCount={activeFilterCount} onClick={handleFilterClick} />
            </div>
            {activeFilterCount > 0 && (
              <div className="mt-2">
                <Text type="caption1" color="secondary">
                  {t('wallet.filters.title')}: {activeFilterCount}
                </Text>
              </div>
            )}
          </div>

          <AnimatePresence mode="wait">
            {isLoading ? (
              <motion.div key="skeleton" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <TransactionListSkeleton />
              </motion.div>
            ) : transactions.length === 0 ? (
              <EmptyState
                icon={<ScrollIcon className="w-7 h-7 text-fg-tertiary" />}
                title={t('wallet.history.empty')}
                description={activeFilterCount > 0 ? t('wallet.history.emptyFiltered') : t('wallet.empty.description')}
                actionLabel={activeFilterCount > 0 ? t('wallet.filters.reset') : undefined}
                onAction={activeFilterCount > 0 ? () => setFilters(EMPTY_FILTERS) : undefined}
              />
            ) : (
              <div>
                <TransactionGroupList
                  transactions={transactions}
                  onItemClick={(txId) => navigate(`/wallet/history/${txId}`)}
                />

                <div ref={sentinelRef} className="h-px" />

                {isFetchingNextPage && (
                  <div className="flex justify-center p-4">
                    <Spinner size="24px" color="accent" />
                  </div>
                )}

                {!hasNextPage && transactions.length > 0 && <EndOfList label={t('wallet.history.endOfList')} />}
              </div>
            )}
          </AnimatePresence>
        </motion.div>
      </AppPageShell>

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
