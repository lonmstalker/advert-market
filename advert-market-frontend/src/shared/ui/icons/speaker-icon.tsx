import type { SVGProps } from 'react';

export function SpeakerIcon(props: SVGProps<SVGSVGElement>) {
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
        d="M12 6L7 10H4C3.44772 10 3 10.4477 3 11V13C3 13.5523 3.44772 14 4 14H7L12 18V6Z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M16 9C17.2 10.2 17.2 13.8 16 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M19 6C21.5 8.5 21.5 15.5 19 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}
