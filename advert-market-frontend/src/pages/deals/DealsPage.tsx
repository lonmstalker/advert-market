import { useInfiniteQuery } from '@tanstack/react-query';
import { Spinner, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { fetchDeals } from '@/features/deals/api/deals';
import { DealListItem } from '@/features/deals/components/DealListItem';
import type { DealRole } from '@/features/deals/types/deal';
import { dealKeys } from '@/shared/api/query-keys';
import { EmptyState, SegmentControl } from '@/shared/ui';
import { staggerChildren } from '@/shared/ui/animations';

const TABS: { value: DealRole; label: string }[] = [
  { value: 'ADVERTISER', label: 'deals.tabs.advertiser' },
  { value: 'OWNER', label: 'deals.tabs.owner' },
];

export default function DealsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [activeRole, setActiveRole] = useState<DealRole>('ADVERTISER');
  const sentinelRef = useRef<HTMLDivElement>(null);

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useInfiniteQuery({
    queryKey: [...dealKeys.lists(), { role: activeRole }],
    queryFn: ({ pageParam }) => fetchDeals({ role: activeRole, cursor: pageParam, limit: 20 }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? (lastPage.nextCursor ?? undefined) : undefined),
  });

  const deals = data?.pages.flatMap((p) => p.items) ?? [];

  const handleIntersection = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      if (entries[0]?.isIntersecting && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    },
    [fetchNextPage, hasNextPage, isFetchingNextPage],
  );

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(handleIntersection, { rootMargin: '200px' });
    observer.observe(el);
    return () => observer.disconnect();
  }, [handleIntersection]);

  const translatedTabs = TABS.map((tab) => ({ value: tab.value, label: t(tab.label) }));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      {/* Header */}
      <div style={{ padding: '16px 16px 0' }}>
        <Text type="title1" weight="bold">
          {t('deals.title')}
        </Text>
      </div>

      {/* Segment Control */}
      <div style={{ padding: '12px 16px' }}>
        <SegmentControl tabs={translatedTabs} active={activeRole} onChange={setActiveRole} />
      </div>

      {/* Content */}
      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
          <Spinner size="32px" color="accent" />
        </div>
      ) : deals.length === 0 ? (
        <EmptyState
          emoji="📬"
          title={t('deals.empty.title')}
          description={t('deals.empty.description')}
          actionLabel={t('deals.empty.cta')}
          onAction={() => navigate('/catalog')}
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

          {!hasNextPage && deals.length > 0 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '20px 24px 8px' }}>
              <div style={{ flex: 1, height: '0.5px', background: 'var(--color-border-separator)' }} />
              <Text type="caption1" color="tertiary" style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>
                {t('deals.endOfList')}
              </Text>
              <div style={{ flex: 1, height: '0.5px', background: 'var(--color-border-separator)' }} />
            </div>
          )}
        </motion.div>
      )}
    </div>
  );
}
