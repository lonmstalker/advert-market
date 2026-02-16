import type { ReactNode } from 'react';

type FixedBottomBarProps = {
  children: ReactNode;
  className?: string;
};

export function FixedBottomBar({ children, className }: FixedBottomBarProps) {
  return (
    <div className={className ? `am-fixed-bottom-bar ${className}` : 'am-fixed-bottom-bar'}>
      {children}
    </div>
  );
}
