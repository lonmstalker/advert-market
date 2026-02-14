import type { SVGProps } from 'react';

export function CoinIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
      <path d="M12 6V18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path
        d="M15.5 9.5C15.5 8.12 13.93 7 12 7C10.07 7 8.5 8.12 8.5 9.5C8.5 10.88 10.07 12 12 12C13.93 12 15.5 13.12 15.5 14.5C15.5 15.88 13.93 17 12 17C10.07 17 8.5 15.88 8.5 14.5"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
