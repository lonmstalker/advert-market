import { Button } from '@telegram-tools/ui-kit';
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
      <Button
        onClick={onClick}
        type={isActive ? 'primary' : 'secondary'}
        className={isActive ? 'am-filter-btn am-filter-btn--active' : 'am-filter-btn'}
        icon={<FilterIcon className="am-filter-btn__icon" />}
      />
      {isActive && <span className="am-filter-btn__badge">{activeCount}</span>}
    </motion.div>
  );
}
