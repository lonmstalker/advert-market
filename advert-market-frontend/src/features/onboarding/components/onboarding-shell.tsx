import type { ReactNode } from 'react';

function joinClasses(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}

type OnboardingShellProps = {
  children: ReactNode;
  footer: ReactNode;
  topAction?: ReactNode;
  contentClassName?: string;
  centerContent?: boolean;
  testId?: string;
};

export function OnboardingShell({
  children,
  footer,
  topAction,
  contentClassName,
  centerContent = false,
  testId,
}: OnboardingShellProps) {
  return (
    <div
      data-testid={testId}
      className="flex flex-col px-[var(--am-onboarding-page-padding)] min-h-[calc(var(--am-viewport-stable-height)-var(--am-onboarding-top-chrome-height,40px))]"
    >
      <div className="w-full max-w-[var(--am-onboarding-max-width)] mx-auto min-h-11 flex justify-end items-center">
        {topAction}
      </div>

      <div
        className={joinClasses(
          'flex-1 min-h-0 w-full max-w-[var(--am-onboarding-max-width)] mx-auto flex flex-col',
          centerContent ? 'justify-center' : 'justify-start',
          contentClassName,
        )}
      >
        {children}
      </div>

      <div className="shrink-0 w-full max-w-[var(--am-onboarding-max-width)] mx-auto pt-3 pb-[calc(var(--am-onboarding-footer-padding-bottom)+var(--am-safe-area-bottom))]">
        {footer}
      </div>
    </div>
  );
}
