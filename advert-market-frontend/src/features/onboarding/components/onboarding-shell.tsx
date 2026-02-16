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
      className="am-onboarding-shell flex flex-col min-h-[calc(var(--am-viewport-stable-height)-var(--am-onboarding-top-chrome-height,40px))]"
    >
      <div className="am-onboarding-shell__container w-full min-h-[calc(var(--am-viewport-stable-height)-var(--am-onboarding-top-chrome-height,40px))] flex flex-col">
        <div className="am-onboarding-shell__top min-h-11 flex justify-end items-center">{topAction}</div>

        <div
          className={joinClasses(
            'am-onboarding-shell__content flex-1 min-h-0 w-full flex flex-col',
            centerContent ? 'justify-center' : 'justify-start',
            contentClassName,
          )}
        >
          {children}
        </div>

        <div className="am-onboarding-shell__footer shrink-0 w-full pt-5 pb-[calc(var(--am-onboarding-footer-padding-bottom)+var(--am-safe-area-bottom))]">
          {footer}
        </div>
      </div>
    </div>
  );
}
