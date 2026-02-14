import { motion } from 'motion/react';
import { useId } from 'react';
import { useHaptic } from '@/shared/hooks/use-haptic';

type Tab<T extends string> = {
  value: T;
  label: string;
};

type SegmentControlProps<T extends string> = {
  tabs: Tab<T>[];
  active: T;
  onChange: (value: T) => void;
};

export function SegmentControl<T extends string>({ tabs, active, onChange }: SegmentControlProps<T>) {
  const id = useId();
  const haptic = useHaptic();
  return (
    <div
      style={{
        display: 'flex',
        position: 'relative',
        borderRadius: 10,
        padding: 2,
        background: 'var(--color-background-base)',
        border: '1px solid var(--color-border-separator)',
      }}
    >
      {tabs.map((tab) => {
        const isActive = active === tab.value;
        return (
          <button
            key={tab.value}
            type="button"
            onClick={() => {
              haptic.selectionChanged();
              onChange(tab.value);
            }}
            style={{
              flex: 1,
              position: 'relative',
              zIndex: 1,
              padding: '8px 16px',
              borderRadius: 8,
              border: 'none',
              cursor: 'pointer',
              background: 'transparent',
              color: isActive ? 'var(--color-static-white)' : 'var(--color-foreground-secondary)',
              fontSize: 14,
              fontWeight: 500,
              fontFamily: 'inherit',
              WebkitTapHighlightColor: 'transparent',
              transition: 'color 0.2s ease',
            }}
          >
            {isActive && (
              <motion.div
                layoutId={`segment-pill-${id}`}
                style={{
                  position: 'absolute',
                  inset: 0,
                  borderRadius: 8,
                  background: 'var(--color-accent-primary)',
                  zIndex: -1,
                }}
                transition={{ type: 'spring', stiffness: 500, damping: 35 }}
              />
            )}
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
