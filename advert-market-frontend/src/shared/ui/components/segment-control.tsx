import { Button } from '@telegram-tools/ui-kit';
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
  const haptic = useHaptic();
  return (
    <div className="am-segment">
      {tabs.map((tab) => {
        const isActive = active === tab.value;
        const className = isActive ? 'am-segment__tab am-segment__tab--active' : 'am-segment__tab';

        return (
          <Button
            key={tab.value}
            type={isActive ? 'primary' : 'secondary'}
            onClick={() => {
              haptic.selectionChanged();
              onChange(tab.value);
            }}
            className={className}
            text={tab.label}
          />
        );
      })}
    </div>
  );
}
