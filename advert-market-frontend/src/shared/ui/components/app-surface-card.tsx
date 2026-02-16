import type { ReactNode } from 'react';

type AppSurfaceCardProps = {
  children: ReactNode;
  className?: string;
  testId?: string;
};

function joinClasses(...classes: Array<string | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}

export function AppSurfaceCard({ children, className, testId }: AppSurfaceCardProps) {
  return (
    <section className={joinClasses('am-surface-card', className)} data-testid={testId}>
      {children}
    </section>
  );
}
