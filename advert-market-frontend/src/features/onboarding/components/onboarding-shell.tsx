import type { CSSProperties, ReactNode } from 'react';

type OnboardingShellProps = {
  children: ReactNode;
  footer: ReactNode;
  topAction?: ReactNode;
  contentStyle?: CSSProperties;
  centerContent?: boolean;
};

export function OnboardingShell({
  children,
  footer,
  topAction,
  contentStyle,
  centerContent = false,
}: OnboardingShellProps) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: 'calc(var(--am-viewport-stable-height) - var(--am-onboarding-top-chrome-height, 40px))',
        paddingLeft: 'var(--am-onboarding-page-padding)',
        paddingRight: 'var(--am-onboarding-page-padding)',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 'var(--am-onboarding-max-width)',
          margin: '0 auto',
          minHeight: 44,
          display: 'flex',
          justifyContent: 'flex-end',
          alignItems: 'center',
        }}
      >
        {topAction}
      </div>

      <div
        style={{
          flex: 1,
          minHeight: 0,
          width: '100%',
          maxWidth: 'var(--am-onboarding-max-width)',
          margin: '0 auto',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: centerContent ? 'center' : 'flex-start',
          ...contentStyle,
        }}
      >
        {children}
      </div>

      <div
        style={{
          flexShrink: 0,
          width: '100%',
          maxWidth: 'var(--am-onboarding-max-width)',
          margin: '0 auto',
          paddingTop: '12px',
          paddingBottom: 'calc(var(--am-onboarding-footer-padding-bottom) + var(--am-safe-area-bottom))',
        }}
      >
        {footer}
      </div>
    </div>
  );
}
