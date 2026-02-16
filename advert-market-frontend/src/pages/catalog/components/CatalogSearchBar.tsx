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
      <div className="flex gap-2 items-center">
        <motion.div
          className="flex-1"
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
          className={`am-filter-button ${activeFilterCount > 0 ? 'am-filter-button--active' : ''}`}
          aria-label={t('catalog.filters.button')}
        >
          <FilterIcon className={`w-5 h-5 ${activeFilterCount > 0 ? 'text-accent' : 'text-fg-secondary'}`} />
          {activeFilterCount > 0 && <span className="am-filter-badge">{activeFilterCount}</span>}
        </Tappable>
      </div>
    </div>
  );
}
