type ToggleChipProps = {
  label: string;
  active: boolean;
  onClick: () => void;
};

export function ToggleChip({ label, active, onClick }: ToggleChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        padding: '6px 14px',
        borderRadius: 16,
        border: active ? 'none' : '1px solid var(--color-border-separator)',
        background: active ? 'var(--color-accent-primary)' : 'var(--color-background-base)',
        color: active ? 'var(--color-static-white)' : 'var(--color-foreground-secondary)',
        fontSize: 13,
        fontWeight: 500,
        whiteSpace: 'nowrap',
        cursor: 'pointer',
        WebkitTapHighlightColor: 'transparent',
        lineHeight: 1.2,
      }}
    >
      {label}
    </button>
  );
}
