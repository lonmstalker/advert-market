type LanguageBadgeSize = 'sm' | 'md';

type LanguageBadgeProps = {
  code: string;
  size?: LanguageBadgeSize;
};

const sizeStyles: Record<LanguageBadgeSize, { padding: string; fontSize: number }> = {
  sm: { padding: '1px 5px', fontSize: 10 },
  md: { padding: '2px 6px', fontSize: 11 },
};

export function LanguageBadge({ code, size = 'md' }: LanguageBadgeProps) {
  const { padding, fontSize } = sizeStyles[size];

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding,
        borderRadius: 4,
        background: 'var(--color-background-secondary)',
        border: '1px solid var(--color-border-separator)',
        fontSize,
        fontWeight: 600,
        color: 'var(--color-foreground-secondary)',
        letterSpacing: '0.02em',
        lineHeight: 1.4,
        textTransform: 'uppercase',
        flexShrink: 0,
      }}
    >
      {code}
    </span>
  );
}
