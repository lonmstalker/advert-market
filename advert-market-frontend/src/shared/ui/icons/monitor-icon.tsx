import type { SVGProps } from 'react';

export function MonitorIcon(props: SVGProps<SVGSVGElement>) {
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
      <rect
        x="3"
        y="4"
        width="18"
        height="13"
        rx="2"
        stroke="currentColor"
        strokeWidth="2"
      />
      <path
        d="M8 21H16"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M12 17V21"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  );
}
