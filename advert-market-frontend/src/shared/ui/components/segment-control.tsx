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
    <div className="am-segment">
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
            data-active={isActive}
            className="am-segment__tab"
          >
            {isActive && (
              <motion.div
                layoutId={`segment-pill-${id}`}
                className="am-segment__pill"
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
