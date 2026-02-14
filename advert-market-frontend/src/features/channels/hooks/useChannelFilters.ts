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
    const next: CatalogFilters = {};

    const q = searchParams.get('q');
    if (q) next.q = q;

    const category = searchParams.get('category');
    if (category) next.category = category;

    const categories = parseArray(searchParams.get('categories'));
    if (categories) next.categories = categories;

    const languages = parseArray(searchParams.get('languages'));
    if (languages) next.languages = languages;

    const minSubs = searchParams.get('minSubs');
    if (minSubs) next.minSubs = Number(minSubs);

    const maxSubs = searchParams.get('maxSubs');
    if (maxSubs) next.maxSubs = Number(maxSubs);

    const minPrice = searchParams.get('minPrice');
    if (minPrice) next.minPrice = Number(minPrice);

    const maxPrice = searchParams.get('maxPrice');
    if (maxPrice) next.maxPrice = Number(maxPrice);

    const sort = searchParams.get('sort') as ChannelSort | null;
    if (sort && channelSortValues.includes(sort)) next.sort = sort;

    return next;
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
