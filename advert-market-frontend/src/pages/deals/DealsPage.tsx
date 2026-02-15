import { useInfiniteQuery } from '@tanstack/react-query';
import { Spinner, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { fetchDeals } from '@/features/deals/api/deals';
import { DealFilterSheet } from '@/features/deals/components/DealFilterSheet';
import { DealListItem } from '@/features/deals/components/DealListItem';
import type { DealRole } from '@/features/deals/types/deal';
import { dealKeys } from '@/shared/api/query-keys';
import { useInfiniteScroll } from '@/shared/hooks/use-infinite-scroll';
import { EmptyState, EndOfList, SegmentControl } from '@/shared/ui';
import { pressScale, staggerChildren } from '@/shared/ui/animations';
import { FilterIcon, MailboxIcon } from '@/shared/ui/icons';

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
      <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
        <div style={{ padding: '16px 16px 0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Text type="title1" weight="bold">
            {t('deals.title')}
          </Text>
          <motion.button
            {...pressScale}
            type="button"
            onClick={() => setSheetOpen(true)}
            style={{
              position: 'relative',
              width: 42,
              height: 42,
              borderRadius: 12,
              border: '1px solid var(--color-border-separator)',
              background: activeFilterCount > 0 ? 'var(--color-accent-primary)' : 'var(--color-background-base)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              padding: 0,
            }}
          >
            <FilterIcon
              style={{
                width: 20,
                height: 20,
                color: activeFilterCount > 0 ? 'var(--color-static-white)' : 'var(--color-foreground-secondary)',
              }}
            />
            {activeFilterCount > 0 && (
              <span
                style={{
                  position: 'absolute',
                  top: -4,
                  right: -4,
                  width: 18,
                  height: 18,
                  borderRadius: '50%',
                  background: 'var(--color-state-destructive)',
                  color: 'var(--color-static-white)',
                  fontSize: 11,
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '2px solid var(--color-background-base)',
                }}
              >
                {activeFilterCount}
              </span>
            )}
          </motion.button>
        </div>

        <div style={{ padding: '12px 16px' }}>
          <SegmentControl tabs={translatedTabs} active={activeRole} onChange={setActiveRole} />
        </div>

        {isLoading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
            <Spinner size="32px" color="accent" />
          </div>
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
            style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12 }}
          >
            {deals.map((deal) => (
              <DealListItem key={deal.id} deal={deal} onClick={() => navigate(`/deals/${deal.id}`)} />
            ))}

            <div ref={sentinelRef} style={{ height: 1 }} />

            {isFetchingNextPage && (
              <div style={{ display: 'flex', justifyContent: 'center', padding: 16 }}>
                <Spinner size="24px" color="accent" />
              </div>
            )}

            {!hasNextPage && deals.length > 0 && <EndOfList label={t('deals.endOfList')} />}
          </motion.div>
        )}
      </div>

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
