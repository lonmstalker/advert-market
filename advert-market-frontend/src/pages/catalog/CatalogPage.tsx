import { useInfiniteQuery } from '@tanstack/react-query';
import { Button, Sheet, SkeletonElement, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import {
  CategoryChipRow,
  ChannelCatalogCard,
  ChannelFiltersContent,
  fetchChannels,
  setFiltersContentProps,
  useChannelFilters,
} from '@/features/channels';
import { channelKeys } from '@/shared/api/query-keys';
import { useDebounce } from '@/shared/hooks/use-debounce';
import { computeCpm, formatCpm } from '@/shared/lib/ton-format';
import { EmptyState } from '@/shared/ui';
import { fadeIn, pressScale, staggerChildren } from '@/shared/ui/animations';
import { FilterIcon, SearchIcon, SearchOffIcon } from '@/shared/ui/icons';

function formatCompact(count: number): string {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`;
  if (count >= 1_000) return `${Math.round(count / 1_000)}K`;
  return String(count);
}

function SkeletonCard() {
  return (
    <div
      style={{
        background: 'var(--color-background-base)',
        border: '1px solid var(--color-border-separator)',
        borderRadius: 16,
        padding: 16,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <SkeletonElement style={{ width: 44, height: 44, borderRadius: '50%' }} />
        <div style={{ flex: 1 }}>
          <SkeletonElement style={{ width: 120, height: 16, borderRadius: 6 }} />
        </div>
        <SkeletonElement style={{ width: 60, height: 16, borderRadius: 6 }} />
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
        <SkeletonElement style={{ width: 40, height: 14, borderRadius: 6 }} />
        <SkeletonElement style={{ width: 60, height: 20, borderRadius: 10 }} />
      </div>
    </div>
  );
}

export default function CatalogPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { filters, setFilters, resetFilters, activeFilterCount } = useChannelFilters();

  const [searchInput, setSearchInput] = useState(filters.q ?? '');
  const [searchFocused, setSearchFocused] = useState(false);
  const debouncedSearch = useDebounce(searchInput, 300);
  const [sheetOpened, setSheetOpened] = useState(false);

  const filtersRef = useRef(filters);
  filtersRef.current = filters;

  useEffect(() => {
    setFilters({ ...filtersRef.current, q: debouncedSearch || undefined });
  }, [debouncedSearch, setFilters]);

  const queryFilters = { ...filters, q: debouncedSearch || undefined };

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading, isError, refetch } = useInfiniteQuery({
    queryKey: channelKeys.list(queryFilters),
    queryFn: ({ pageParam }) => fetchChannels({ ...queryFilters, cursor: pageParam, limit: 20 }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? (lastPage.nextCursor ?? undefined) : undefined),
  });

  const observerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = observerRef.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 0.1 },
    );

    observer.observe(el);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const channels = data?.pages.flatMap((page) => page.items) ?? [];
  const totalCount = data?.pages[0]?.total;

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
    setFiltersContentProps({
      currentFilters: filters,
      onApply: (next) => {
        setFilters(next);
        setSheetOpened(false);
      },
      onReset: () => {
        resetFilters();
        setSearchInput('');
        setSheetOpened(false);
      },
    });
    setSheetOpened(true);
  }, [filters, setFilters, resetFilters]);

  const sheets = { filters: ChannelFiltersContent };

  return (
    <div style={{ paddingBottom: 24 }}>
      {/* Search bar area */}
      <div
        style={{
          padding: '16px 16px 12px',
          position: 'sticky',
          top: 0,
          zIndex: 5,
          background: 'var(--color-background-secondary)',
        }}
      >
        <div
          style={{
            display: 'flex',
            gap: 8,
            alignItems: 'center',
          }}
        >
          <motion.div
            style={{ flex: 1 }}
            animate={{
              scale: searchFocused ? 1.01 : 1,
            }}
            transition={{ duration: 0.15, ease: 'easeOut' }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: '10px 12px',
                borderRadius: 12,
                background: 'var(--color-background-base)',
                border: `1.5px solid ${searchFocused ? 'var(--color-accent-primary)' : 'var(--color-border-separator)'}`,
                transition: 'border-color 0.2s ease',
              }}
            >
              <SearchIcon
                style={{
                  width: 18,
                  height: 18,
                  color: searchFocused ? 'var(--color-accent-primary)' : 'var(--color-foreground-tertiary)',
                  flexShrink: 0,
                  transition: 'color 0.2s ease',
                }}
              />
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onFocus={() => setSearchFocused(true)}
                onBlur={() => setSearchFocused(false)}
                placeholder={t('catalog.search.placeholder')}
                style={{
                  flex: 1,
                  border: 'none',
                  outline: 'none',
                  background: 'transparent',
                  fontSize: 15,
                  color: 'var(--color-foreground-primary)',
                  lineHeight: 1.3,
                  fontFamily: 'inherit',
                }}
              />
            </div>
          </motion.div>
          <motion.button
            {...pressScale}
            type="button"
            onClick={handleOpenFilters}
            style={{
              position: 'relative',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 42,
              height: 42,
              borderRadius: 12,
              border: activeFilterCount > 0
                ? '1.5px solid var(--color-accent-primary)'
                : '1.5px solid var(--color-border-separator)',
              background: activeFilterCount > 0
                ? 'color-mix(in srgb, var(--color-accent-primary) 8%, transparent)'
                : 'var(--color-background-base)',
              cursor: 'pointer',
              WebkitTapHighlightColor: 'transparent',
              flexShrink: 0,
              padding: 0,
            }}
            aria-label={t('catalog.filters.button')}
          >
            <FilterIcon
              style={{
                width: 20,
                height: 20,
                color: activeFilterCount > 0
                  ? 'var(--color-accent-primary)'
                  : 'var(--color-foreground-secondary)',
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
                  background: 'var(--color-accent-primary)',
                  color: 'var(--color-static-white)',
                  fontSize: 11,
                  fontWeight: 700,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  lineHeight: 1,
                  pointerEvents: 'none',
                }}
              >
                {activeFilterCount}
              </span>
            )}
          </motion.button>
        </div>
      </div>

      <CategoryChipRow
        selected={filters.categories ?? (filters.category ? [filters.category] : [])}
        onSelect={(categories) => setFilters({ ...filters, categories, category: undefined })}
      />

      <AnimatePresence mode="wait">
        {isLoading ? (
          <motion.div
            key="skeleton"
            {...fadeIn}
            style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12 }}
          >
            {[1, 2, 3].map((i) => (
              <SkeletonCard key={i} />
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
              icon={<SearchOffIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
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
          <motion.div key="list" {...staggerChildren}>
            {totalCount != null && summary && (
              <div style={{ padding: '8px 16px 4px' }}>
                <Text type="footnote" color="secondary">
                  {summary.avgCpm != null
                    ? t('catalog.summary', {
                        count: totalCount,
                        avgCpm: formatCpm(summary.avgCpm),
                        minSubs: formatCompact(summary.minSubs),
                        maxSubs: formatCompact(summary.maxSubs),
                      })
                    : t('catalog.filters.show', { count: totalCount })}
                </Text>
              </div>
            )}

            <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
              {channels.map((channel) => (
                <ChannelCatalogCard
                  key={channel.id}
                  channel={channel}
                  onClick={() => navigate(`/catalog/channels/${channel.id}`)}
                />
              ))}
            </div>

            {isFetchingNextPage && (
              <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
                <SkeletonCard />
              </div>
            )}

            <div ref={observerRef} style={{ height: 1 }} />

            {!hasNextPage && channels.length > 0 && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.3 }}
                style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '20px 40px 8px' }}
              >
                <div style={{ flex: 1, height: '0.5px', background: 'var(--color-border-separator)' }} />
                <Text type="caption1" color="tertiary" style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>
                  {t('catalog.endOfList')}
                </Text>
                <div style={{ flex: 1, height: '0.5px', background: 'var(--color-border-separator)' }} />
              </motion.div>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      <Sheet sheets={sheets} activeSheet="filters" opened={sheetOpened} onClose={() => setSheetOpened(false)} />
    </div>
  );
}