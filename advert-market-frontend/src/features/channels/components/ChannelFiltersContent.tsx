import { useQuery } from '@tanstack/react-query';
import { Button, Input, Text } from '@telegram-tools/ui-kit';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { channelKeys } from '@/shared/api/query-keys';
import { parseTonToNano } from '@/shared/lib/ton-format';
import { fetchCategories, fetchChannelCount } from '../api/channels';
import type { CatalogFilters, ChannelSort } from '../types/channel';
import { channelSortValues } from '../types/channel';
import { useChannelFiltersContext } from './ChannelFiltersContext';

const AVAILABLE_LANGUAGES = [
  { code: 'ru', label: 'RU' },
  { code: 'en', label: 'EN' },
  { code: 'uk', label: 'UA' },
  { code: 'uz', label: 'UZ' },
  { code: 'kz', label: 'KZ' },
];

function FilterSection({ children }: { children: React.ReactNode }) {
  return (
    <div
      style={{
        borderBottom: '1px solid var(--color-border-separator)',
        paddingBottom: 16,
      }}
    >
      {children}
    </div>
  );
}

function ToggleChip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        padding: '6px 14px',
        borderRadius: 16,
        border: active ? 'none' : '1px solid var(--color-border-separator)',
        background: active ? 'var(--color-accent-primary)' : 'var(--color-background-base)',
        color: active ? 'var(--color-static-white)' : 'var(--color-foreground-secondary)',
        fontSize: 13,
        fontWeight: 500,
        whiteSpace: 'nowrap',
        cursor: 'pointer',
        WebkitTapHighlightColor: 'transparent',
        lineHeight: 1.2,
      }}
    >
      {label}
    </button>
  );
}

export function ChannelFiltersContent() {
  const filtersContext = useChannelFiltersContext();

  const { t, i18n } = useTranslation();
  const lang = i18n.language;

  const { currentFilters, onApply, onReset } = filtersContext;

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
  const [sort, setSort] = useState<string | null>(currentFilters.sort ?? null);

  const { data: categories = [] } = useQuery({
    queryKey: channelKeys.categories(),
    queryFn: fetchCategories,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const draftFilters: CatalogFilters = useMemo(
    () => ({
      q: currentFilters.q,
      categories: selectedCategories.length > 0 ? selectedCategories : undefined,
      languages: selectedLanguages.length > 0 ? selectedLanguages : undefined,
      minSubs: minSubs ? Number(minSubs) : undefined,
      maxSubs: maxSubs ? Number(maxSubs) : undefined,
      minPrice: minPrice ? Number(parseTonToNano(minPrice)) : undefined,
      maxPrice: maxPrice ? Number(parseTonToNano(maxPrice)) : undefined,
      sort: (sort as ChannelSort) || undefined,
    }),
    [selectedCategories, selectedLanguages, minSubs, maxSubs, minPrice, maxPrice, sort, currentFilters.q],
  );

  const { data: count } = useQuery({
    queryKey: channelKeys.count(draftFilters),
    queryFn: () => fetchChannelCount(draftFilters),
    placeholderData: (prev) => prev,
  });

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

  const sortOptions = [
    { label: t('catalog.filters.sortDefault'), value: null },
    ...channelSortValues.map((sv) => ({
      label: t(`catalog.filters.sort.${sv}`),
      value: sv,
    })),
  ];

  const handleApply = () => {
    onApply(draftFilters);
  };

  const handleReset = () => {
    setSelectedCategories([]);
    setSelectedLanguages([]);
    setMinSubs('');
    setMaxSubs('');
    setMinPrice('');
    setMaxPrice('');
    setSort(null);
    onReset();
  };

  const showButtonText =
    count != null
      ? count === 0
        ? t('catalog.filters.nothingFound')
        : t('catalog.filters.showChannels', { count })
      : t('catalog.filters.showButton');

  const sortedCategories = [...categories].sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Text type="title2" weight="bold">
        {t('catalog.filters.title')}
      </Text>

      {/* Categories multi-select */}
      <FilterSection>
        <Text type="body" weight="medium" style={{ marginBottom: 8 }}>
          {t('catalog.filters.topic')}
        </Text>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {sortedCategories.map((cat) => (
            <ToggleChip
              key={cat.slug}
              label={cat.localizedName[lang] ?? cat.localizedName.ru ?? cat.slug}
              active={selectedCategories.includes(cat.slug)}
              onClick={() => toggleCategory(cat.slug)}
            />
          ))}
        </div>
      </FilterSection>

      {/* Languages multi-select */}
      <FilterSection>
        <Text type="body" weight="medium" style={{ marginBottom: 8 }}>
          {t('catalog.filters.language')}
        </Text>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {AVAILABLE_LANGUAGES.map((l) => (
            <ToggleChip
              key={l.code}
              label={t(`catalog.languages.full.${l.code}`, { defaultValue: l.label })}
              active={selectedLanguages.includes(l.code)}
              onClick={() => toggleLanguage(l.code)}
            />
          ))}
        </div>
      </FilterSection>

      <FilterSection>
        <Text type="body" weight="medium" style={{ marginBottom: 8 }}>
          {t('catalog.filters.subscribers')}
        </Text>
        <div style={{ display: 'flex', gap: 8 }}>
          <Input value={minSubs} onChange={setMinSubs} numeric placeholder={t('catalog.filters.from')} />
          <Input value={maxSubs} onChange={setMaxSubs} numeric placeholder={t('catalog.filters.to')} />
        </div>
      </FilterSection>

      <FilterSection>
        <Text type="body" weight="medium" style={{ marginBottom: 8 }}>
          {t('catalog.filters.pricePerPost')}
        </Text>
        <div style={{ display: 'flex', gap: 8 }}>
          <Input value={minPrice} onChange={setMinPrice} placeholder={`${t('catalog.filters.from')} TON`} />
          <Input value={maxPrice} onChange={setMaxPrice} placeholder={`${t('catalog.filters.to')} TON`} />
        </div>
      </FilterSection>

      <div>
        <Text type="body" weight="medium" style={{ marginBottom: 8 }}>
          {t('catalog.filters.sortLabel')}
        </Text>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {sortOptions.map((opt) => (
            <ToggleChip
              key={opt.value ?? '__default'}
              label={opt.label}
              active={sort === opt.value}
              onClick={() => setSort(opt.value)}
            />
          ))}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 24 }}>
        {hasActiveFilters && (
          <div style={{ flex: 1 }}>
            <Button text={t('catalog.filters.reset')} type="secondary" onClick={handleReset} />
          </div>
        )}
        <div style={{ flex: 2 }}>
          <Button text={showButtonText} type="primary" disabled={count === 0} onClick={handleApply} />
        </div>
      </div>
    </div>
  );
}
