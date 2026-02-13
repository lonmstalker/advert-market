import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { channelKeys } from '@/shared/api/query-keys';
import { fetchCategories } from '../api/channels';
import type { Category } from '../types/channel';

type CategoryChipRowProps = {
  selected: string[];
  onSelect: (slugs: string[]) => void;
};

function Chip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
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
        flexShrink: 0,
        lineHeight: 1.2,
      }}
    >
      {label}
    </button>
  );
}

function getCategoryName(cat: Category, lang: string): string {
  return cat.localizedName[lang] ?? cat.localizedName.ru ?? cat.slug;
}

export function CategoryChipRow({ selected, onSelect }: CategoryChipRowProps) {
  const { t, i18n } = useTranslation();
  const lang = i18n.language;

  const { data: categories } = useQuery({
    queryKey: channelKeys.categories(),
    queryFn: fetchCategories,
    staleTime: 5 * 60 * 1000,
  });

  if (!categories || categories.length === 0) return null;

  const sorted = [...categories].sort((a, b) => a.sortOrder - b.sortOrder);

  const isAllSelected = selected.length === 0;

  const handleToggle = (slug: string) => {
    if (selected.includes(slug)) {
      onSelect(selected.filter((s) => s !== slug));
    } else {
      onSelect([...selected, slug]);
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        gap: 8,
        padding: '0 16px 8px',
        overflowX: 'auto',
        scrollbarWidth: 'none',
        WebkitOverflowScrolling: 'touch',
      }}
    >
      <Chip
        label={t('catalog.filters.topicAll')}
        active={isAllSelected}
        onClick={() => onSelect([])}
      />
      {sorted.map((cat: Category) => (
        <Chip
          key={cat.slug}
          label={getCategoryName(cat, lang)}
          active={selected.includes(cat.slug)}
          onClick={() => handleToggle(cat.slug)}
        />
      ))}
    </div>
  );
}