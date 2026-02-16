import { useInfiniteQuery } from '@tanstack/react-query';
import { Spinner } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { fetchDeals } from '@/features/deals/api/deals';
import { DealFilterSheet } from '@/features/deals/components/DealFilterSheet';
import { DealListItem } from '@/features/deals/components/DealListItem';
import { DealListSkeleton } from '@/features/deals/components/DealListSkeleton';
import type { DealRole } from '@/features/deals/types/deal';
import { dealKeys } from '@/shared/api/query-keys';
import { useInfiniteScroll } from '@/shared/hooks/use-infinite-scroll';
import { AppPageShell, AppSectionHeader, EmptyState, EndOfList, FilterButton, SegmentControl } from '@/shared/ui';
import { staggerChildren } from '@/shared/ui/animations';
import { MailboxIcon } from '@/shared/ui/icons';

const TABS: { value: DealRole; label: string }[] = [
  { value: 'ADVERTISER', label: 'deals.tabs.advertiser' },
  { value: 'OWNER', label: 'deals.tabs.owner' },
];

export default function DealsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [activeRole, setActiveRole] = useState<DealRole>('ADVERTISER');
  const [filterStatuses, setFilterStatuses] = useState<Set<string>>(new Set());
  const [sheetOpen, setSheetOpen] = useState(false);

  const statusesArray = useMemo(() => [...filterStatuses], [filterStatuses]);

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useInfiniteQuery({
    queryKey: [...dealKeys.lists(), { role: activeRole, statuses: statusesArray }],
    queryFn: ({ pageParam }) =>
      fetchDeals({
        role: activeRole,
        cursor: pageParam,
        limit: 20,
        statuses: statusesArray.length > 0 ? statusesArray : undefined,
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? (lastPage.nextCursor ?? undefined) : undefined),
  });

  const deals = useMemo(() => data?.pages.flatMap((p) => p.items) ?? [], [data]);

  const sentinelRef = useInfiniteScroll({ hasNextPage, isFetchingNextPage, fetchNextPage });

  const translatedTabs = TABS.map((tab) => ({ value: tab.value, label: t(tab.label) }));

  const activeFilterCount = filterStatuses.size;

  return (
    <>
      <AppPageShell testId="deals-page-shell">
        <AppSectionHeader
          title={t('deals.title')}
          action={<FilterButton activeCount={activeFilterCount} onClick={() => setSheetOpen(true)} />}
        />

        <div className="am-surface-row p-1.5">
          <SegmentControl tabs={translatedTabs} active={activeRole} onChange={setActiveRole} />
        </div>

        <AnimatePresence mode="wait">
          {isLoading ? (
            <motion.div key="skeleton" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <DealListSkeleton />
            </motion.div>
          ) : deals.length === 0 ? (
            <EmptyState
              icon={<MailboxIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
              title={t('deals.empty.title')}
              description={activeFilterCount > 0 ? t('deals.empty.filteredDescription') : t('deals.empty.description')}
              actionLabel={activeFilterCount > 0 ? t('common.reset') : t('deals.empty.cta')}
              onAction={activeFilterCount > 0 ? () => setFilterStatuses(new Set()) : () => navigate('/catalog')}
            />
          ) : (
            <motion.div
              {...staggerChildren}
              initial="initial"
              animate="animate"
              className="am-deal-grid"
            >
              {deals.map((deal) => (
                <DealListItem key={deal.id} deal={deal} onClick={() => navigate(`/deals/${deal.id}`)} />
              ))}

              <div ref={sentinelRef} className="h-px" />

              {isFetchingNextPage && (
                <div className="flex justify-center p-4">
                  <Spinner size="24px" color="accent" />
                </div>
              )}

              {!hasNextPage && deals.length > 0 && <EndOfList label={t('deals.endOfList')} />}
            </motion.div>
          )}
        </AnimatePresence>
      </AppPageShell>

      <DealFilterSheet
        open={sheetOpen}
        onClose={() => setSheetOpen(false)}
        activeStatuses={filterStatuses}
        onApply={setFilterStatuses}
        onReset={() => {
          setFilterStatuses(new Set());
          setSheetOpen(false);
        }}
      />
    </>
  );
}
