import { Outlet, useLocation } from 'react-router';
import { StepIndicator } from '@/features/onboarding/components/step-indicator';
import { BackButtonHandler } from '@/shared/ui';

export function OnboardingLayout() {
  const { pathname } = useLocation();
  const isTour = pathname === '/onboarding/tour';

  return (
    <>
      <BackButtonHandler />
      <div style={{ opacity: isTour ? 0.4 : 1, transition: 'opacity 0.25s' }}>
        <StepIndicator />
      </div>
      <Outlet />
    </>
  );
}
