import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { channelKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { Chip } from '@/shared/ui';
import { fetchCategories } from '../api/channels';
import type { Category } from '../types/channel';

type CategoryChipRowProps = {
  selected: string[];
  onSelect: (slugs: string[]) => void;
};

function getCategoryName(cat: Category, lang: string): string {
  return cat.localizedName[lang] ?? cat.localizedName.ru ?? cat.slug;
}

export function CategoryChipRow({ selected, onSelect }: CategoryChipRowProps) {
  const { t, i18n } = useTranslation();
  const lang = i18n.language;
  const haptic = useHaptic();

  const { data: categories } = useQuery({
    queryKey: channelKeys.categories(),
    queryFn: fetchCategories,
    staleTime: 5 * 60 * 1000,
  });

  if (!categories || categories.length === 0) return null;

  const sorted = [...categories].sort((a, b) => a.sortOrder - b.sortOrder);

  const isAllSelected = selected.length === 0;

  const handleToggle = (slug: string) => {
    haptic.impactOccurred('light');
    if (selected.includes(slug)) {
      onSelect(selected.filter((s) => s !== slug));
    } else {
      onSelect([...selected, slug]);
    }
  };

  return (
    <fieldset aria-label={t('catalog.filters.topic')} className="am-chip-row border-none m-0">
      <Chip
        label={t('catalog.filters.topicAll')}
        active={isAllSelected}
        onClick={() => {
          haptic.impactOccurred('light');
          onSelect([]);
        }}
      />
      {sorted.map((cat: Category) => (
        <Chip
          key={cat.slug}
          label={getCategoryName(cat, lang)}
          active={selected.includes(cat.slug)}
          onClick={() => handleToggle(cat.slug)}
        />
      ))}
    </fieldset>
  );
}
