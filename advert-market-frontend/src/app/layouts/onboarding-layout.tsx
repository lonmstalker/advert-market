import { Outlet, useLocation } from 'react-router';
import { StepIndicator } from '@/features/onboarding/components/step-indicator';
import { BackButtonHandler } from '@/shared/ui';

export function OnboardingLayout() {
  const { pathname } = useLocation();
  const isTour = pathname === '/onboarding/tour';

  return (
    <>
      <BackButtonHandler />
      <div style={{ paddingTop: 'var(--am-safe-area-top)' }}>
        {isTour ? <div aria-hidden="true" style={{ height: 40 }} /> : <StepIndicator />}
      </div>
      <Outlet />
    </>
  );
}
