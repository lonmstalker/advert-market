import { motion } from 'motion/react';
import { pressScale } from '../animations';
import { FilterIcon } from '../icons';

type FilterButtonProps = {
  activeCount: number;
  onClick: () => void;
};

export function FilterButton({ activeCount, onClick }: FilterButtonProps) {
  return (
    <motion.button
      {...pressScale}
      type="button"
      onClick={onClick}
      style={{
        position: 'relative',
        width: 42,
        height: 42,
        borderRadius: 12,
        border: '1px solid var(--color-border-separator)',
        background: activeCount > 0 ? 'var(--color-accent-primary)' : 'var(--color-background-base)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
        padding: 0,
      }}
    >
      <FilterIcon
        style={{
          width: 20,
          height: 20,
          color: activeCount > 0 ? 'var(--color-static-white)' : 'var(--color-foreground-secondary)',
        }}
      />
      {activeCount > 0 && (
        <span
          style={{
            position: 'absolute',
            top: -4,
            right: -4,
            width: 18,
            height: 18,
            borderRadius: '50%',
            background: 'var(--color-state-destructive)',
            color: 'var(--color-static-white)',
            fontSize: 11,
            fontWeight: 700,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            border: '2px solid var(--color-background-base)',
          }}
        >
          {activeCount}
        </span>
      )}
    </motion.button>
  );
}
