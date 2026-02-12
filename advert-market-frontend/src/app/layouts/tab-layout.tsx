import { Outlet } from 'react-router';
import { BackButtonHandler, BottomTabs } from '@/shared/ui';

export function TabLayout() {
  return (
    <>
      <BackButtonHandler />
      <div style={{ paddingBottom: 'calc(56px + env(safe-area-inset-bottom))' }}>
        <Outlet />
      </div>
      <BottomTabs />
    </>
  );
}
