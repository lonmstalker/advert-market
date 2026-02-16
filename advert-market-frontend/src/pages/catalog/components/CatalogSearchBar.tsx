import { easeOut } from 'motion';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { SearchInput, Tappable } from '@/shared/ui';
import { FilterIcon } from '@/shared/ui/icons';

type CatalogSearchBarProps = {
  searchInput: string;
  onSearchChange: (value: string) => void;
  searchFocused: boolean;
  onFocus: () => void;
  onBlur: () => void;
  activeFilterCount: number;
  onOpenFilters: () => void;
};

export function CatalogSearchBar({
  searchInput,
  onSearchChange,
  searchFocused,
  onFocus,
  onBlur,
  activeFilterCount,
  onOpenFilters,
}: CatalogSearchBarProps) {
  const { t } = useTranslation();

  return (
    <div className="am-toolbar">
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <motion.div
          style={{ flex: 1 }}
          animate={{ scale: searchFocused ? 1.01 : 1 }}
          transition={{ duration: 0.15, ease: easeOut }}
        >
          <SearchInput
            value={searchInput}
            onChange={onSearchChange}
            onFocus={onFocus}
            onBlur={onBlur}
            focused={searchFocused}
            placeholder={t('catalog.search.placeholder')}
          />
        </motion.div>
        <Tappable
          onClick={onOpenFilters}
          style={{
            position: 'relative',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 42,
            height: 42,
            borderRadius: 'var(--am-radius-control)',
            border:
              activeFilterCount > 0 ? '1.5px solid var(--color-accent-primary)' : '1.5px solid var(--am-card-border)',
            background:
              activeFilterCount > 0
                ? 'color-mix(in srgb, var(--color-accent-primary) 14%, transparent)'
                : 'var(--am-control-surface)',
            boxShadow: 'var(--am-card-shadow)',
            cursor: 'pointer',
            WebkitTapHighlightColor: 'transparent',
            flexShrink: 0,
            padding: 0,
          }}
          aria-label={t('catalog.filters.button')}
        >
          <FilterIcon
            style={{
              width: 20,
              height: 20,
              color: activeFilterCount > 0 ? 'var(--color-accent-primary)' : 'var(--color-foreground-secondary)',
            }}
          />
          {activeFilterCount > 0 && (
            <span
              style={{
                position: 'absolute',
                top: -4,
                right: -4,
                width: 18,
                height: 18,
                borderRadius: '50%',
                background: 'var(--color-accent-primary)',
                color: 'var(--color-static-white)',
                fontSize: 11,
                fontWeight: 700,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                lineHeight: 1,
                pointerEvents: 'none',
              }}
            >
              {activeFilterCount}
            </span>
          )}
        </Tappable>
      </div>
    </div>
  );
}
