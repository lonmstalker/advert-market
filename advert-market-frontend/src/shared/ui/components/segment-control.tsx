import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { pressScale } from '@/shared/ui/animations';

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
  const haptic = useHaptic();
  return (
    <div className="am-segment" role="tablist">
      {tabs.map((tab) => {
        const isActive = active === tab.value;

        return (
          <motion.button
            key={tab.value}
            {...pressScale}
            type="button"
            role="tab"
            aria-selected={isActive}
            className={isActive ? 'am-segment__tab am-segment__tab--active' : 'am-segment__tab'}
            onClick={() => {
              haptic.selectionChanged();
              onChange(tab.value);
            }}
          >
            <Text type="subheadline2" weight="bold">
              {tab.label}
            </Text>
          </motion.button>
        );
      })}
    </div>
  );
}
