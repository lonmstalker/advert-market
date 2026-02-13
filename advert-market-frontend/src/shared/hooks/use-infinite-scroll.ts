import { useCallback, useEffect, useRef } from 'react';

type InfiniteScrollOptions = {
  hasNextPage: boolean | undefined;
  isFetchingNextPage: boolean;
  fetchNextPage: () => void;
  rootMargin?: string;
  threshold?: number;
};

/**
 * Returns a ref to attach to a sentinel element.
 * When the sentinel becomes visible, triggers fetchNextPage.
 */
export function useInfiniteScroll({
  hasNextPage,
  isFetchingNextPage,
  fetchNextPage,
  rootMargin = '200px',
  threshold = 0,
}: InfiniteScrollOptions) {
  const sentinelRef = useRef<HTMLDivElement>(null);

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
    const observer = new IntersectionObserver(handleIntersection, { rootMargin, threshold });
    observer.observe(el);
    return () => observer.disconnect();
  }, [handleIntersection, rootMargin, threshold]);

  return sentinelRef;
}
