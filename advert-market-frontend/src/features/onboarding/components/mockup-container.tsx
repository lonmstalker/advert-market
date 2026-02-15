import type { ReactNode } from 'react';

type MockupContainerProps = {
  children: ReactNode;
};

export function MockupContainer({ children }: MockupContainerProps) {
  return (
    <div
      style={{
        borderRadius: '16px',
        border: '1px solid var(--color-border-separator)',
        backgroundColor: 'var(--color-background-secondary)',
        padding: '16px',
        maxHeight: '420px',
        overflowY: 'auto',
        scrollbarWidth: 'none',
        WebkitOverflowScrolling: 'touch',
      }}
    >
      {children}
    </div>
  );
}
