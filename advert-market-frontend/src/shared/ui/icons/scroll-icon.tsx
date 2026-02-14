import type { SVGProps } from 'react';

export function ScrollIcon(props: SVGProps<SVGSVGElement>) {
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
        d="M8 21H12C12.5304 21 13.0391 20.7893 13.4142 20.4142C13.7893 20.0391 14 19.5304 14 19V5C14 4.46957 13.7893 3.96086 13.4142 3.58579C13.0391 3.21071 12.5304 3 12 3H6C5.46957 3 4.96086 3.21071 4.58579 3.58579C4.21071 3.96086 4 4.46957 4 5V17"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M4 17C4 17.7956 4.31607 18.5587 4.87868 19.1213C5.44129 19.6839 6.20435 20 7 20H8C8 20.7956 8.31607 21.5587 8.87868 22.1213C9.44129 22.6839 10.2044 23 11 23H12C12.7956 23 13.5587 22.6839 14.1213 22.1213"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M18 7H20C20.5304 7 21.0391 7.21071 21.4142 7.58579C21.7893 7.96086 22 8.46957 22 9V17C22 17.7956 21.6839 18.5587 21.1213 19.1213C20.5587 19.6839 19.7957 20 19 20H8"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M18 3V7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M7 7H11" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M7 11H11" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}
