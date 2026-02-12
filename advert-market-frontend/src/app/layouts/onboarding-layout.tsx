import { Outlet } from 'react-router';
import { BackButtonHandler } from '@/shared/ui';

export function OnboardingLayout() {
  return (
    <>
      <BackButtonHandler />
      <Outlet />
    </>
  );
}
