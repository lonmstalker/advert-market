import type { SVGProps } from 'react';

export function FormatStrikethroughIcon(props: SVGProps<SVGSVGElement>) {
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
      <path d="M4 12H20" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path
        d="M17.5 7.5C17.5 4.46 14.76 3 12 3C9.24 3 6.5 4.46 6.5 7.5C6.5 12 17.5 12 17.5 16.5C17.5 19.54 14.76 21 12 21C9.24 21 6.5 19.54 6.5 16.5"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
