import { useInfiniteQuery } from '@tanstack/react-query';
import { Sheet, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import {
  CategoryChipRow,
  ChannelCardSkeleton,
  ChannelCatalogCard,
  ChannelFiltersContent,
  ChannelFiltersProvider,
  fetchChannels,
  useChannelFilters,
} from '@/features/channels';
import { channelKeys } from '@/shared/api/query-keys';
import { useDebounce } from '@/shared/hooks/use-debounce';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useInfiniteScroll } from '@/shared/hooks/use-infinite-scroll';
import { formatCompactNumber } from '@/shared/lib/format-number';
import { computeCpm, formatCpm } from '@/shared/lib/ton-format';
import { AppPageShell, EmptyState, EndOfList } from '@/shared/ui';
import { fadeIn, staggerChildren } from '@/shared/ui/animations';
import { SearchOffIcon } from '@/shared/ui/icons';
import { CatalogSearchBar } from './components/CatalogSearchBar';

export default function CatalogPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { filters, setFilters, resetFilters, activeFilterCount } = useChannelFilters();

  const [searchInput, setSearchInput] = useState(filters.q ?? '');
  const [searchFocused, setSearchFocused] = useState(false);
  const debouncedSearch = useDebounce(searchInput, 300);
  const [sheetOpened, setSheetOpened] = useState(false);
  const haptic = useHaptic();

  // ref breaks the filters→effect→setFilters→filters dependency cycle
  const filtersRef = useRef(filters);
  filtersRef.current = filters;

  useEffect(() => {
    const next = { ...filtersRef.current };
    if (debouncedSearch) {
      next.q = debouncedSearch;
    } else {
      delete next.q;
    }
    setFilters(next);
  }, [debouncedSearch, setFilters]);

  const queryFilters = useMemo(() => {
    const next = { ...filters };
    if (debouncedSearch) {
      next.q = debouncedSearch;
    } else {
      delete next.q;
    }
    return next;
  }, [filters, debouncedSearch]);

  const queryKeyParams = useMemo(
    () => ({
      q: queryFilters.q,
      category: queryFilters.category,
      categories: queryFilters.categories?.join(','),
      languages: queryFilters.languages?.join(','),
      minSubs: queryFilters.minSubs,
      maxSubs: queryFilters.maxSubs,
      minPrice: queryFilters.minPrice,
      maxPrice: queryFilters.maxPrice,
      sort: queryFilters.sort,
    }),
    [
      queryFilters.q,
      queryFilters.category,
      queryFilters.categories,
      queryFilters.languages,
      queryFilters.minSubs,
      queryFilters.maxSubs,
      queryFilters.minPrice,
      queryFilters.maxPrice,
      queryFilters.sort,
    ],
  );

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading, isError, refetch } = useInfiniteQuery({
    queryKey: channelKeys.list(queryKeyParams),
    queryFn: ({ pageParam }) => fetchChannels({ ...queryFilters, cursor: pageParam, limit: 20 }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? (lastPage.nextCursor ?? undefined) : undefined),
  });

  const observerRef = useInfiniteScroll({ hasNextPage, isFetchingNextPage, fetchNextPage, threshold: 0.1 });

  const channels = useMemo(() => data?.pages.flatMap((page) => page.items) ?? [], [data]);
  const totalCount = channels.length;

  const summary = useMemo(() => {
    if (channels.length === 0) return null;
    const subs = channels.map((c) => c.subscriberCount);
    const minSubs = Math.min(...subs);
    const maxSubs = Math.max(...subs);

    const cpms: number[] = [];
    for (const ch of channels) {
      if (ch.pricePerPostNano != null && ch.avgViews && ch.avgViews > 0) {
        const cpm = computeCpm(ch.pricePerPostNano, ch.avgViews);
        if (cpm != null) cpms.push(cpm);
      }
    }
    const avgCpm = cpms.length > 0 ? cpms.reduce((a, b) => a + b, 0) / cpms.length : null;

    return { minSubs, maxSubs, avgCpm };
  }, [channels]);

  const handleOpenFilters = useCallback(() => {
    haptic.impactOccurred('light');
    setSheetOpened(true);
  }, [haptic]);

  const sheets = { filters: ChannelFiltersContent };

  return (
    <AppPageShell testId="catalog-page-shell">
      <CatalogSearchBar
        searchInput={searchInput}
        onSearchChange={setSearchInput}
        searchFocused={searchFocused}
        onFocus={() => setSearchFocused(true)}
        onBlur={() => setSearchFocused(false)}
        activeFilterCount={activeFilterCount}
        onOpenFilters={handleOpenFilters}
      />

      <CategoryChipRow
        selected={filters.categories ?? (filters.category ? [filters.category] : [])}
        onSelect={(categories) => setFilters({ ...filters, categories, category: undefined })}
      />

      <AnimatePresence mode="wait">
        {isLoading ? (
          <motion.div key="skeleton" {...fadeIn} className="am-catalog-grid">
            {[1, 2, 3].map((i) => (
              <ChannelCardSkeleton key={i} />
            ))}
          </motion.div>
        ) : isError ? (
          <motion.div key="error" {...fadeIn}>
            <EmptyState
              title={t('common.error')}
              description={t('errors.server')}
              actionLabel={t('common.retry')}
              onAction={() => refetch()}
            />
          </motion.div>
        ) : channels.length === 0 ? (
          <motion.div key="empty" {...fadeIn}>
            <EmptyState
              icon={<SearchOffIcon className="w-7 h-7 text-fg-tertiary" />}
              title={t('catalog.empty.title')}
              description={t('catalog.empty.description')}
              actionLabel={t('catalog.empty.cta')}
              onAction={() => {
                resetFilters();
                setSearchInput('');
              }}
            />
          </motion.div>
        ) : (
          <motion.div key="list" {...staggerChildren} initial="initial" animate="animate">
            {summary && (
              <div className="py-3 px-1">
                <Text type="footnote" color="secondary">
                  {summary.avgCpm != null
                    ? t('catalog.summary', {
                        count: totalCount,
                        avgCpm: formatCpm(summary.avgCpm),
                        minSubs: formatCompactNumber(summary.minSubs),
                        maxSubs: formatCompactNumber(summary.maxSubs),
                      })
                    : t('catalog.filters.show', { count: totalCount })}
                </Text>
              </div>
            )}

            <div className="am-catalog-grid">
              {channels.map((channel) => (
                <ChannelCatalogCard
                  key={channel.id}
                  channel={channel}
                  onClick={() => navigate(`/catalog/channels/${channel.id}`)}
                />
              ))}
            </div>

            {isFetchingNextPage && (
              <div className="am-catalog-grid pt-4">
                <ChannelCardSkeleton />
              </div>
            )}

            <div ref={observerRef} className="h-px" />

            {!hasNextPage && channels.length > 0 && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.3 }}>
                <EndOfList label={t('catalog.endOfList')} />
              </motion.div>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      <ChannelFiltersProvider
        currentFilters={filters}
        onApply={(next) => {
          setFilters(next);
          setSheetOpened(false);
        }}
        onReset={() => {
          resetFilters();
          setSearchInput('');
          setSheetOpened(false);
        }}
      >
        <Sheet sheets={sheets} activeSheet="filters" opened={sheetOpened} onClose={() => setSheetOpened(false)} />
      </ChannelFiltersProvider>
    </AppPageShell>
  );
}
