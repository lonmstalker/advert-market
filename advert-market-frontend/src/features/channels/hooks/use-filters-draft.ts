import { useMemo, useState } from 'react';
import { parseTonToNano } from '@/shared/lib/ton-format';
import type { CatalogFilters, ChannelSort } from '../types/channel';

export function useFiltersDraft(currentFilters: CatalogFilters) {
  const initialCategories = currentFilters.categories ?? (currentFilters.category ? [currentFilters.category] : []);

  const [selectedCategories, setSelectedCategories] = useState<string[]>(initialCategories);
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>(currentFilters.languages ?? []);
  const [minSubs, setMinSubs] = useState(currentFilters.minSubs?.toString() ?? '');
  const [maxSubs, setMaxSubs] = useState(currentFilters.maxSubs?.toString() ?? '');
  const [minPrice, setMinPrice] = useState(
    currentFilters.minPrice ? (currentFilters.minPrice / 1_000_000_000).toString() : '',
  );
  const [maxPrice, setMaxPrice] = useState(
    currentFilters.maxPrice ? (currentFilters.maxPrice / 1_000_000_000).toString() : '',
  );
  const [sort, setSort] = useState<ChannelSort | null>(currentFilters.sort ?? null);

  const draftFilters: CatalogFilters = useMemo(() => {
    const next: CatalogFilters = {};

    if (currentFilters.q) next.q = currentFilters.q;

    if (selectedCategories.length > 0) {
      next.category = selectedCategories[0];
      next.categories = selectedCategories;
    }

    if (selectedLanguages.length > 0) {
      next.languages = selectedLanguages;
    }

    if (minSubs) next.minSubs = Number(minSubs);
    if (maxSubs) next.maxSubs = Number(maxSubs);
    if (minPrice) next.minPrice = Number(parseTonToNano(minPrice));
    if (maxPrice) next.maxPrice = Number(parseTonToNano(maxPrice));

    if (sort) next.sort = sort;

    return next;
  }, [selectedCategories, selectedLanguages, minSubs, maxSubs, minPrice, maxPrice, sort, currentFilters.q]);

  const hasActiveFilters =
    selectedCategories.length > 0 ||
    selectedLanguages.length > 0 ||
    minSubs !== '' ||
    maxSubs !== '' ||
    minPrice !== '' ||
    maxPrice !== '' ||
    sort != null;

  const toggleCategory = (slug: string) => {
    setSelectedCategories((prev) => (prev.includes(slug) ? prev.filter((s) => s !== slug) : [...prev, slug]));
  };

  const toggleLanguage = (code: string) => {
    setSelectedLanguages((prev) => (prev.includes(code) ? prev.filter((s) => s !== code) : [...prev, code]));
  };

  const handleReset = () => {
    setSelectedCategories([]);
    setSelectedLanguages([]);
    setMinSubs('');
    setMaxSubs('');
    setMinPrice('');
    setMaxPrice('');
    setSort(null);
  };

  return {
    selectedCategories,
    selectedLanguages,
    minSubs,
    setMinSubs,
    maxSubs,
    setMaxSubs,
    minPrice,
    setMinPrice,
    maxPrice,
    setMaxPrice,
    sort,
    setSort,
    draftFilters,
    hasActiveFilters,
    toggleCategory,
    toggleLanguage,
    handleReset,
  };
}
