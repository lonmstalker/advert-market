import type { CSSProperties, ReactNode } from 'react';

type FixedBottomBarProps = {
  children: ReactNode;
  style?: CSSProperties;
};

export function FixedBottomBar({ children, style }: FixedBottomBarProps) {
  return (
    <div
      style={{
        position: 'fixed',
        left: 0,
        right: 0,
        bottom: 0,
        padding: '12px 16px calc(16px + var(--am-safe-area-bottom))',
        background: 'var(--color-background-base)',
        borderTop: '0.5px solid var(--color-border-separator)',
        zIndex: 20,
        ...style,
      }}
    >
      {children}
    </div>
  );
}
