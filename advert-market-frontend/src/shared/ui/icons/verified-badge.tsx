type VerifiedBadgeProps = {
  size?: number;
  className?: string;
};

export function VerifiedBadge({ size = 16, className }: VerifiedBadgeProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      className={className}
      aria-label="Verified"
    >
      <title>Verified</title>
      <circle cx="12" cy="12" r="12" fill="#3390EC" />
      <path d="M9.5 12.5l2 2 4-5" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
