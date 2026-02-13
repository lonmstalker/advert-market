import type { SVGProps } from 'react';

export function MegaphoneIcon(props: SVGProps<SVGSVGElement>) {
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
      <path
        d="M18 8C19.1046 8 20 8.89543 20 10V10C20 11.1046 19.1046 12 18 12"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M4 9H6L14 5V17L6 13H4C3.44772 13 3 12.5523 3 12V10C3 9.44772 3.44772 9 4 9Z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M7 13V17C7 18.1046 7.89543 19 9 19V19C10.1046 19 11 18.1046 11 17V15"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
