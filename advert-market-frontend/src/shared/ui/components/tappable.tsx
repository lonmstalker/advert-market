import type { CSSProperties, ReactNode } from 'react';

type TappableProps = {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  style?: CSSProperties;
  'aria-label'?: string;
  'aria-pressed'?: boolean;
};

export function Tappable({ children, onClick, disabled, style, ...ariaProps }: TappableProps) {
  return (
    <button type="button" onClick={onClick} disabled={disabled} style={style} {...ariaProps}>
      {children}
    </button>
  );
}
