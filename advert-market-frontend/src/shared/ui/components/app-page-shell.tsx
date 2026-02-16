import type { ReactNode } from 'react';

type AppPageShellProps = {
  children: ReactNode;
  className?: string;
  stackClassName?: string;
  withTabsPadding?: boolean;
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
  testId,
}: AppPageShellProps) {
  return (
    <main className={joinClasses('am-page', withTabsPadding && 'am-page-with-tabs', className)} data-testid={testId}>
      <div className={joinClasses('am-page-stack', stackClassName)}>{children}</div>
    </main>
  );
}
