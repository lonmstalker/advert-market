import { Outlet } from 'react-router';
import { StepIndicator } from '@/features/onboarding/components/step-indicator';
import { BackButtonHandler } from '@/shared/ui';

export function OnboardingLayout() {
  return (
    <>
      <BackButtonHandler />
      <StepIndicator />
      <Outlet />
    </>
  );
}
