import { motion } from 'motion/react';
import { pressScale } from '../animations';
import { FilterIcon } from '../icons';

type FilterButtonProps = {
  activeCount: number;
  onClick: () => void;
};

export function FilterButton({ activeCount, onClick }: FilterButtonProps) {
  const isActive = activeCount > 0;
  return (
    <motion.div {...pressScale} className="am-filter-btn-shell">
      <button
        type="button"
        onClick={onClick}
        className={isActive ? 'am-filter-btn am-filter-btn--active' : 'am-filter-btn'}
        aria-label={`Filters${isActive ? ` (${activeCount} active)` : ''}`}
      >
        <FilterIcon className="am-filter-btn__icon" />
      </button>
      {isActive && <span className="am-filter-btn__badge">{activeCount}</span>}
    </motion.div>
  );
}
