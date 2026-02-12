import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router';
import type { CatalogFilters, ChannelSort } from '../types/channel';
import { channelSortValues } from '../types/channel';

export function useChannelFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  const filters: CatalogFilters = useMemo(() => {
    const sort = searchParams.get('sort') as ChannelSort | null;
    return {
      q: searchParams.get('q') || undefined,
      topic: searchParams.get('topic') || undefined,
      minSubs: searchParams.get('minSubs') ? Number(searchParams.get('minSubs')) : undefined,
      maxSubs: searchParams.get('maxSubs') ? Number(searchParams.get('maxSubs')) : undefined,
      minPrice: searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : undefined,
      maxPrice: searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : undefined,
      sort: sort && channelSortValues.includes(sort) ? sort : undefined,
    };
  }, [searchParams]);

  const setFilters = useCallback(
    (next: CatalogFilters) => {
      setSearchParams(
        (prev) => {
          const params = new URLSearchParams(prev);
          const entries: [string, string | undefined][] = [
            ['q', next.q],
            ['topic', next.topic],
            ['minSubs', next.minSubs?.toString()],
            ['maxSubs', next.maxSubs?.toString()],
            ['minPrice', next.minPrice?.toString()],
            ['maxPrice', next.maxPrice?.toString()],
            ['sort', next.sort],
          ];
          for (const [key, value] of entries) {
            if (value) {
              params.set(key, value);
            } else {
              params.delete(key);
            }
          }
          return params;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  const resetFilters = useCallback(() => {
    setSearchParams({}, { replace: true });
  }, [setSearchParams]);

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (filters.topic) count++;
    if (filters.minSubs || filters.maxSubs) count++;
    if (filters.minPrice || filters.maxPrice) count++;
    if (filters.sort && filters.sort !== 'relevance') count++;
    return count;
  }, [filters]);

  return { filters, setFilters, resetFilters, activeFilterCount };
}
