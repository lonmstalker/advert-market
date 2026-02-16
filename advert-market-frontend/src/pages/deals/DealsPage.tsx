import { useInfiniteQuery, useQueries, useQuery } from '@tanstack/react-query';
import { Spinner } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { fetchChannelDetail } from '@/features/channels';
import { fetchDeals } from '@/features/deals/api/deals';
import { DealFilterSheet } from '@/features/deals/components/DealFilterSheet';
import { DealListItem } from '@/features/deals/components/DealListItem';
import { DealListSkeleton } from '@/features/deals/components/DealListSkeleton';
import { mapDealDtoToViewModel } from '@/features/deals/lib/deal-mapper';
import type { DealChannelMetadata, DealRole } from '@/features/deals/types/deal';
import { fetchProfile } from '@/shared/api/auth';
import { channelKeys, dealKeys, profileKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useInfiniteScroll } from '@/shared/hooks/use-infinite-scroll';
import { AppPageShell, AppSectionHeader, EmptyState, EndOfList, FilterButton, SegmentControl } from '@/shared/ui';
import { staggerChildren } from '@/shared/ui/animations';
import { MailboxIcon } from '@/shared/ui/icons';

const TABS: { value: DealRole; label: string }[] = [
  { value: 'ADVERTISER', label: 'deals.tabs.advertiser' },
  { value: 'OWNER', label: 'deals.tabs.owner' },
];

function mapChannelMetadata(detail: Awaited<ReturnType<typeof fetchChannelDetail>>): DealChannelMetadata {
  const firstRule = detail.pricingRules[0];
  return {
    title: detail.title,
    username: detail.username ?? null,
    postType: firstRule?.postTypes?.[0] ?? null,
    durationHours: null,
    postFrequencyHours: detail.postFrequencyHours ?? null,
  };
}

export default function DealsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [activeRole, setActiveRole] = useState<DealRole>('ADVERTISER');
  const [filterStatuses, setFilterStatuses] = useState<Set<string>>(new Set());
  const [sheetOpen, setSheetOpen] = useState(false);
  const haptic = useHaptic();

  const { data: profile, isLoading: isProfileLoading } = useQuery({
    queryKey: profileKeys.me,
    queryFn: fetchProfile,
  });

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading: isDealsLoading,
  } = useInfiniteQuery({
    queryKey: [...dealKeys.lists(), 'all'],
    queryFn: ({ pageParam }) =>
      fetchDeals({
        cursor: pageParam,
        limit: 20,
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? (lastPage.nextCursor ?? undefined) : undefined),
  });

  const dealDtos = useMemo(() => data?.pages.flatMap((p) => p.items) ?? [], [data]);

  const channelIds = useMemo(() => [...new Set(dealDtos.map((deal) => deal.channelId))], [dealDtos]);

  const channelQueries = useQueries({
    queries: channelIds.map((channelId) => ({
      queryKey: channelKeys.detail(channelId),
      queryFn: () => fetchChannelDetail(channelId),
      staleTime: 5 * 60 * 1000,
    })),
  });

  const channelMetaById = useMemo(() => {
    const map = new Map<number, DealChannelMetadata>();
    channelIds.forEach((channelId, index) => {
      const detail = channelQueries[index]?.data;
      if (detail) {
        map.set(channelId, mapChannelMetadata(detail));
      }
    });
    return map;
  }, [channelIds, channelQueries]);

  const deals = useMemo(() => {
    if (!profile?.id) return [];

    const mapped = dealDtos
      .map((deal) =>
        mapDealDtoToViewModel(deal, {
          profileId: profile.id,
          channel: channelMetaById.get(deal.channelId) ?? null,
        }),
      )
      .filter((deal) => deal.role === activeRole);

    if (filterStatuses.size === 0) {
      return mapped;
    }

    return mapped.filter((deal) => filterStatuses.has(deal.status));
  }, [activeRole, channelMetaById, dealDtos, filterStatuses, profile?.id]);

  const sentinelRef = useInfiniteScroll({ hasNextPage, isFetchingNextPage, fetchNextPage });

  const translatedTabs = TABS.map((tab) => ({ value: tab.value, label: t(tab.label) }));

  const activeFilterCount = filterStatuses.size;
  const isLoading = isProfileLoading || isDealsLoading;

  return (
    <>
      <AppPageShell testId="deals-page-shell">
        <AppSectionHeader
          title={t('deals.title')}
          action={
            <FilterButton
              activeCount={activeFilterCount}
              onClick={() => {
                haptic.impactOccurred('light');
                setSheetOpen(true);
              }}
            />
          }
        />

        <div className="am-surface-row p-2.5">
          <SegmentControl tabs={translatedTabs} active={activeRole} onChange={setActiveRole} />
        </div>

        <AnimatePresence mode="wait">
          {isLoading ? (
            <motion.div key="skeleton" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <DealListSkeleton />
            </motion.div>
          ) : deals.length === 0 ? (
            <EmptyState
              icon={<MailboxIcon className="w-7 h-7 text-fg-tertiary" />}
              title={t('deals.empty.title')}
              description={activeFilterCount > 0 ? t('deals.empty.filteredDescription') : t('deals.empty.description')}
              actionLabel={activeFilterCount > 0 ? t('common.reset') : t('deals.empty.cta')}
              onAction={activeFilterCount > 0 ? () => setFilterStatuses(new Set()) : () => navigate('/catalog')}
            />
          ) : (
            <motion.div {...staggerChildren} initial="initial" animate="animate" className="am-deal-grid">
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
