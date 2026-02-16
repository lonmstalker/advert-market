type RuleIndicatorProps = {
  allowed: boolean;
};

export function RuleIndicator({ allowed }: RuleIndicatorProps) {
  return (
    <span
      className={`inline-flex items-center justify-center w-[18px] h-[18px] rounded-full shrink-0 mt-px ${
        allowed ? 'bg-soft-success' : 'bg-soft-destructive'
      }`}
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
