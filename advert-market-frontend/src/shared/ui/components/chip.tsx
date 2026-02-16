import { Text } from '@telegram-tools/ui-kit';
import { Tappable } from './tappable';

type ChipProps = {
  label: string;
  active: boolean;
  onClick: () => void;
  variant?: 'pill' | 'rounded';
};

export function Chip({ label, active, onClick, variant = 'pill' }: ChipProps) {
  const className = ['am-chip', variant === 'rounded' ? 'am-chip--rounded' : '', active ? 'am-chip--active' : '']
    .filter(Boolean)
    .join(' ');

  return (
    <Tappable aria-pressed={active} data-active={active ? 'true' : 'false'} onClick={onClick} className={className}>
      <Text type="caption1" color={active ? 'white' : 'secondary'}>
        {label}
      </Text>
    </Tappable>
  );
}
