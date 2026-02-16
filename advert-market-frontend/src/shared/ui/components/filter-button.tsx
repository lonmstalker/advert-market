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
    <motion.button
      {...pressScale}
      type="button"
      onClick={onClick}
      data-active={isActive}
      className="am-filter-btn"
    >
      <FilterIcon className="am-filter-btn__icon" />
      {isActive && <span className="am-filter-btn__badge">{activeCount}</span>}
    </motion.button>
  );
}
