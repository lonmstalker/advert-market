type RuleIndicatorProps = {
  allowed: boolean;
};

export function RuleIndicator({ allowed }: RuleIndicatorProps) {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: 18,
        height: 18,
        borderRadius: '50%',
        background: allowed
          ? 'color-mix(in srgb, var(--color-state-success) 12%, transparent)'
          : 'color-mix(in srgb, var(--color-state-destructive) 12%, transparent)',
        flexShrink: 0,
        marginTop: 1,
      }}
    >
      <svg width="10" height="10" viewBox="0 0 10 10" fill="none" aria-hidden="true">
        {allowed ? (
          <path
            d="M2 5.5L4 7.5L8 3"
            stroke="var(--color-state-success)"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        ) : (
          <path
            d="M3 3L7 7M7 3L3 7"
            stroke="var(--color-state-destructive)"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}
      </svg>
    </span>
  );
}
