import { useQuery } from '@tanstack/react-query';
import { Button, Input, Select, Text } from '@telegram-tools/ui-kit';
import { useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { channelKeys } from '@/shared/api/query-keys';
import { parseTonToNano } from '@/shared/lib/ton-format';
import { fetchChannelCount, fetchChannelTopics } from '../api/channels';
import type { CatalogFilters, ChannelSort } from '../types/channel';
import { channelSortValues } from '../types/channel';

type ChannelFiltersContentProps = {
  currentFilters: CatalogFilters;
  onApply: (filters: CatalogFilters) => void;
  onReset: () => void;
};

let filtersContentProps: ChannelFiltersContentProps | null = null;

export function setFiltersContentProps(props: ChannelFiltersContentProps) {
  filtersContentProps = props;
}

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
  const propsRef = useRef(filtersContentProps);
  propsRef.current = filtersContentProps;
  const initial = filtersContentProps;

  const { t } = useTranslation();

  const [topic, setTopic] = useState<string | null>(initial?.currentFilters.topic ?? null);
  const [minSubs, setMinSubs] = useState(initial?.currentFilters.minSubs?.toString() ?? '');
  const [maxSubs, setMaxSubs] = useState(initial?.currentFilters.maxSubs?.toString() ?? '');
  const [minPrice, setMinPrice] = useState(
    initial?.currentFilters.minPrice ? (initial.currentFilters.minPrice / 1_000_000_000).toString() : '',
  );
  const [maxPrice, setMaxPrice] = useState(
    initial?.currentFilters.maxPrice ? (initial.currentFilters.maxPrice / 1_000_000_000).toString() : '',
  );
  const [sort, setSort] = useState<string | null>(initial?.currentFilters.sort ?? null);

  const { data: topics = [] } = useQuery({
    queryKey: channelKeys.topics(),
    queryFn: fetchChannelTopics,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const draftFilters: CatalogFilters = useMemo(
    () => ({
      q: propsRef.current?.currentFilters.q,
      topic: topic || undefined,
      minSubs: minSubs ? Number(minSubs) : undefined,
      maxSubs: maxSubs ? Number(maxSubs) : undefined,
      minPrice: minPrice ? Number(parseTonToNano(minPrice)) : undefined,
      maxPrice: maxPrice ? Number(parseTonToNano(maxPrice)) : undefined,
      sort: (sort as ChannelSort) || undefined,
    }),
    [topic, minSubs, maxSubs, minPrice, maxPrice, sort],
  );

  const { data: count } = useQuery({
    queryKey: channelKeys.count(draftFilters),
    queryFn: () => fetchChannelCount(draftFilters),
    placeholderData: (prev) => prev,
  });

  const hasActiveFilters =
    topic != null || minSubs !== '' || maxSubs !== '' || minPrice !== '' || maxPrice !== '' || sort != null;

  const topicOptions = [
    { label: t('catalog.filters.topicAll'), value: null },
    ...topics.map((tp) => ({ label: tp.name, value: tp.slug })),
  ];

  const sortOptions = [
    { label: t('catalog.filters.sortDefault'), value: null },
    ...channelSortValues.map((sv) => ({
      label: t(`catalog.filters.sort.${sv}`),
      value: sv,
    })),
  ];

  const handleApply = () => {
    propsRef.current?.onApply(draftFilters);
  };

  const handleReset = () => {
    setTopic(null);
    setMinSubs('');
    setMaxSubs('');
    setMinPrice('');
    setMaxPrice('');
    setSort(null);
    propsRef.current?.onReset();
  };

  const showButtonText =
    count != null
      ? count === 0
        ? t('catalog.filters.nothingFound')
        : t('catalog.filters.showChannels', { count })
      : t('catalog.filters.showButton');

  return (
    <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Text type="title2" weight="bold">
        {t('catalog.filters.title')}
      </Text>

      <FilterSection>
        <Text type="body" weight="medium" style={{ marginBottom: 8 }}>
          {t('catalog.filters.topic')}
        </Text>
        <Select options={topicOptions} value={topic} onChange={setTopic} />
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
        <Select options={sortOptions} value={sort} onChange={setSort} />
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
