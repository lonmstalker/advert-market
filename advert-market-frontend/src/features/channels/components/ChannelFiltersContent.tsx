import { useQuery } from '@tanstack/react-query';
import { Button, Input, Text } from '@telegram-tools/ui-kit';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { channelKeys } from '@/shared/api/query-keys';
import { Chip } from '@/shared/ui';
import { fetchCategories, fetchChannelCount } from '../api/channels';
import { useFiltersDraft } from '../hooks/use-filters-draft';
import { channelSortValues } from '../types/channel';
import { useChannelFiltersContext } from './ChannelFiltersContext';

const AVAILABLE_LANGUAGES = [
  { code: 'ru', label: 'RU' },
  { code: 'en', label: 'EN' },
  { code: 'uk', label: 'UA' },
  { code: 'uz', label: 'UZ' },
  { code: 'kz', label: 'KZ' },
];

const fieldsetReset = { border: 'none', margin: 0, padding: 0 } as const;

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

export function ChannelFiltersContent() {
  const { currentFilters, onApply, onReset } = useChannelFiltersContext();
  const { t, i18n } = useTranslation();
  const lang = i18n.language;

  const draft = useFiltersDraft(currentFilters);

  const { data: categories = [] } = useQuery({
    queryKey: channelKeys.categories(),
    queryFn: fetchCategories,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const countParams = useMemo(
    () => ({
      q: draft.draftFilters.q,
      category: draft.draftFilters.category,
      minSubs: draft.draftFilters.minSubs,
      maxSubs: draft.draftFilters.maxSubs,
      minPrice: draft.draftFilters.minPrice,
      maxPrice: draft.draftFilters.maxPrice,
      categories: draft.draftFilters.categories?.join(','),
      languages: draft.draftFilters.languages?.join(','),
    }),
    [
      draft.draftFilters.q,
      draft.draftFilters.category,
      draft.draftFilters.minSubs,
      draft.draftFilters.maxSubs,
      draft.draftFilters.minPrice,
      draft.draftFilters.maxPrice,
      draft.draftFilters.categories,
      draft.draftFilters.languages,
    ],
  );

  const { data: count } = useQuery({
    queryKey: channelKeys.count(countParams),
    queryFn: () => fetchChannelCount(draft.draftFilters),
    placeholderData: (prev) => prev,
  });

  const sortOptions = [
    { label: t('catalog.filters.sortDefault'), value: null },
    ...channelSortValues.map((sv) => ({
      label: t(`catalog.filters.sort.${sv}`),
      value: sv,
    })),
  ];

  const handleApply = () => {
    onApply(draft.draftFilters);
  };

  const handleReset = () => {
    draft.handleReset();
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

      <FilterSection>
        <div style={{ marginBottom: 8 }}>
          <Text type="body" weight="medium">
            {t('catalog.filters.topic')}
          </Text>
        </div>
        <fieldset
          aria-label={t('catalog.filters.topic')}
          style={{ ...fieldsetReset, display: 'flex', flexWrap: 'wrap', gap: 6 }}
        >
          {sortedCategories.map((cat) => (
            <Chip
              key={cat.slug}
              variant="rounded"
              label={cat.localizedName[lang] ?? cat.localizedName.ru ?? cat.slug}
              active={draft.selectedCategories.includes(cat.slug)}
              onClick={() => draft.toggleCategory(cat.slug)}
            />
          ))}
        </fieldset>
      </FilterSection>

      <FilterSection>
        <div style={{ marginBottom: 8 }}>
          <Text type="body" weight="medium">
            {t('catalog.filters.language')}
          </Text>
        </div>
        <fieldset
          aria-label={t('catalog.filters.language')}
          style={{ ...fieldsetReset, display: 'flex', flexWrap: 'wrap', gap: 6 }}
        >
          {AVAILABLE_LANGUAGES.map((l) => (
            <Chip
              key={l.code}
              variant="rounded"
              label={t(`catalog.languages.full.${l.code}`, { defaultValue: l.label })}
              active={draft.selectedLanguages.includes(l.code)}
              onClick={() => draft.toggleLanguage(l.code)}
            />
          ))}
        </fieldset>
      </FilterSection>

      <FilterSection>
        <div style={{ marginBottom: 8 }}>
          <Text type="body" weight="medium">
            {t('catalog.filters.subscribers')}
          </Text>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Input value={draft.minSubs} onChange={draft.setMinSubs} numeric placeholder={t('catalog.filters.from')} />
          <Input value={draft.maxSubs} onChange={draft.setMaxSubs} numeric placeholder={t('catalog.filters.to')} />
        </div>
      </FilterSection>

      <FilterSection>
        <div style={{ marginBottom: 8 }}>
          <Text type="body" weight="medium">
            {t('catalog.filters.pricePerPost')}
          </Text>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Input value={draft.minPrice} onChange={draft.setMinPrice} placeholder={`${t('catalog.filters.from')} TON`} />
          <Input value={draft.maxPrice} onChange={draft.setMaxPrice} placeholder={`${t('catalog.filters.to')} TON`} />
        </div>
      </FilterSection>

      <div>
        <div style={{ marginBottom: 8 }}>
          <Text type="body" weight="medium">
            {t('catalog.filters.sortLabel')}
          </Text>
        </div>
        <fieldset
          aria-label={t('catalog.filters.sortLabel')}
          style={{ ...fieldsetReset, display: 'flex', flexWrap: 'wrap', gap: 6 }}
        >
          {sortOptions.map((opt) => (
            <Chip
              key={opt.value ?? '__default'}
              variant="rounded"
              label={opt.label}
              active={draft.sort === opt.value}
              onClick={() => draft.setSort(opt.value)}
            />
          ))}
        </fieldset>
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 24 }}>
        {draft.hasActiveFilters && (
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
