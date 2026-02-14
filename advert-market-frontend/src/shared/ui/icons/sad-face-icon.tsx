import type { SVGProps } from 'react';

export function SadFaceIcon(props: SVGProps<SVGSVGElement>) {
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
      <path d="M9 9H9.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M15 9H15.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path
        d="M8 15C8.5 13.5 10 12.5 12 12.5C14 12.5 15.5 13.5 16 15"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
