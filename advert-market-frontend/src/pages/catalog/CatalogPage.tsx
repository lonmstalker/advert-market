import { useInfiniteQuery } from '@tanstack/react-query';
import { Button, Input, Sheet, SkeletonElement, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import {
  ChannelCatalogCard,
  ChannelFiltersContent,
  fetchChannels,
  setFiltersContentProps,
  useChannelFilters,
} from '@/features/channels';
import { channelKeys } from '@/shared/api/query-keys';
import { useDebounce } from '@/shared/hooks/use-debounce';
import { EmptyState } from '@/shared/ui';
import { fadeIn, pressScale, staggerChildren } from '@/shared/ui/animations';

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
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
          <SkeletonElement style={{ width: 120, height: 16, borderRadius: 6 }} />
          <SkeletonElement style={{ width: 80, height: 12, borderRadius: 6 }} />
        </div>
        <SkeletonElement style={{ width: 60, height: 16, borderRadius: 6 }} />
      </div>
      <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 4 }}>
        <SkeletonElement style={{ width: '100%', height: 12, borderRadius: 6 }} />
        <SkeletonElement style={{ width: '70%', height: 12, borderRadius: 6 }} />
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
        <SkeletonElement style={{ width: 60, height: 20, borderRadius: 10 }} />
        <SkeletonElement style={{ width: 40, height: 14, borderRadius: 6 }} />
      </div>
    </div>
  );
}

export default function CatalogPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { filters, setFilters, resetFilters, activeFilterCount } = useChannelFilters();

  const [searchInput, setSearchInput] = useState(filters.q ?? '');
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
          padding: '12px 16px',
          display: 'flex',
          gap: 8,
          position: 'sticky',
          top: 0,
          zIndex: 5,
          background: 'var(--color-background-secondary)',
        }}
      >
        <div style={{ flex: 1 }}>
          <Input value={searchInput} onChange={setSearchInput} placeholder={t('catalog.search.placeholder')} />
        </div>
        <div style={{ flexShrink: 0, position: 'relative' }}>
          <motion.div {...pressScale}>
            <Button text={t('catalog.filters.button')} type="secondary" onClick={handleOpenFilters} />
          </motion.div>
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
                color: '#fff',
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
        </div>
      </div>

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
              emoji="⚠️"
              title={t('common.error')}
              description={t('errors.server')}
              actionLabel={t('common.retry')}
              onAction={() => refetch()}
            />
          </motion.div>
        ) : channels.length === 0 ? (
          <motion.div key="empty" {...fadeIn}>
            <EmptyState
              emoji="🔍"
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
            {totalCount != null && (
              <div style={{ padding: '8px 16px 4px' }}>
                <Text type="footnote" color="secondary">
                  {t('catalog.filters.show', { count: totalCount })}
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
          </motion.div>
        )}
      </AnimatePresence>

      <Sheet sheets={sheets} activeSheet="filters" opened={sheetOpened} onClose={() => setSheetOpened(false)} />
    </div>
  );
}
