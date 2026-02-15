import type { CSSProperties } from 'react';

type ChipProps = {
  label: string;
  active: boolean;
  onClick: () => void;
  variant?: 'pill' | 'rounded';
  style?: CSSProperties;
};

export function Chip({ label, active, onClick, variant = 'pill', style }: ChipProps) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      style={{
        padding: '6px 14px',
        borderRadius: variant === 'pill' ? 100 : 16,
        border: active ? 'none' : '1px solid var(--color-border-separator)',
        background: active ? 'var(--color-accent-primary)' : 'var(--color-background-base)',
        color: active ? 'var(--color-static-white)' : 'var(--color-foreground-secondary)',
        fontSize: 13,
        fontWeight: 500,
        whiteSpace: 'nowrap',
        cursor: 'pointer',
        WebkitTapHighlightColor: 'transparent',
        flexShrink: 0,
        lineHeight: 1.2,
        ...style,
      }}
    >
      {label}
    </button>
  );
}
