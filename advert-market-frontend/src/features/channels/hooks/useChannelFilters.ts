import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router';
import type { CatalogFilters, ChannelSort } from '../types/channel';
import { channelSortValues } from '../types/channel';

function parseArray(value: string | null): string[] | undefined {
  if (!value) return undefined;
  const arr = value.split(',').filter(Boolean);
  return arr.length > 0 ? arr : undefined;
}

function serializeArray(arr: string[] | undefined): string | undefined {
  if (!arr || arr.length === 0) return undefined;
  return arr.join(',');
}

export function useChannelFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  const filters: CatalogFilters = useMemo(() => {
    const sort = searchParams.get('sort') as ChannelSort | null;
    const categories = parseArray(searchParams.get('categories'));
    const languages = parseArray(searchParams.get('languages'));
    return {
      q: searchParams.get('q') || undefined,
      category: searchParams.get('category') || undefined,
      categories,
      languages,
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
            ['category', next.category],
            ['categories', serializeArray(next.categories)],
            ['languages', serializeArray(next.languages)],
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
    if (filters.category || (filters.categories && filters.categories.length > 0)) count++;
    if (filters.languages && filters.languages.length > 0) count++;
    if (filters.minSubs || filters.maxSubs) count++;
    if (filters.minPrice || filters.maxPrice) count++;
    if (filters.sort && filters.sort !== 'relevance') count++;
    return count;
  }, [filters]);

  return { filters, setFilters, resetFilters, activeFilterCount };
}