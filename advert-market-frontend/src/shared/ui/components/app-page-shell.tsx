import type { ReactNode } from 'react';

type AppPageShellProps = {
  children: ReactNode;
  className?: string;
  stackClassName?: string;
  withTabsPadding?: boolean;
  variant?: 'default' | 'finance';
  testId?: string;
};

function joinClasses(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}

export function AppPageShell({
  children,
  className,
  stackClassName,
  withTabsPadding = true,
  variant = 'default',
  testId,
}: AppPageShellProps) {
  const isFinance = variant === 'finance';
  const pageClass = isFinance ? 'am-page am-page--finance' : 'am-page';
  const stackClass = isFinance ? 'am-finance-stack' : 'am-page-stack';

  return (
    <main
      className={joinClasses(pageClass, withTabsPadding && 'am-page-with-tabs', className)}
      data-testid={testId}
    >
      <div className={joinClasses(stackClass, stackClassName)}>{children}</div>
    </main>
  );
}
