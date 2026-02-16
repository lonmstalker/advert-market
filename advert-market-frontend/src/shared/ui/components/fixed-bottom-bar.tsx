import type { ReactNode } from 'react';

type FixedBottomBarProps = {
  children: ReactNode;
  className?: string;
};

export function FixedBottomBar({ children, className }: FixedBottomBarProps) {
  return (
    <div className="am-fixed-bottom-bar">
      <div className={className ? `am-fixed-bottom-bar__inner ${className}` : 'am-fixed-bottom-bar__inner'}>
        {children}
      </div>
    </div>
  );
}
