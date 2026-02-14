import type { SVGProps } from 'react';

export function VerifiedIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      role={props['aria-label'] ? 'img' : undefined}
      aria-hidden={!props['aria-label']}
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      {props['aria-label'] && <title>{props['aria-label']}</title>}
      <path
        d="M8 1L9.545 2.694L11.708 2.292L12.109 4.455L14 5.545L13.09 7.545L14 9.545L12.109 10.636L11.708 12.8L9.545 12.397L8 14.091L6.455 12.397L4.292 12.8L3.891 10.636L2 9.545L2.91 7.545L2 5.545L3.891 4.455L4.292 2.292L6.455 2.694L8 1Z"
        fill="currentColor"
      />
      <path
        d="M5.5 8L7 9.5L10.5 6"
        stroke="var(--color-static-white, #fff)"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
