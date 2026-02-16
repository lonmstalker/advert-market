type ChipProps = {
  label: string;
  active: boolean;
  onClick: () => void;
  variant?: 'pill' | 'rounded';
};

export function Chip({ label, active, onClick, variant = 'pill' }: ChipProps) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      data-active={active}
      className={variant === 'rounded' ? 'am-chip am-chip--rounded' : 'am-chip'}
    >
      {label}
    </button>
  );
}
