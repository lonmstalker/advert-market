import { Outlet } from 'react-router';
import { BackButtonHandler, BottomTabs } from '@/shared/ui';

export function TabLayout() {
  return (
    <>
      <BackButtonHandler />
      <div style={{ paddingBottom: 'calc(56px + var(--am-safe-area-bottom))' }}>
        <Outlet />
      </div>
      <BottomTabs />
    </>
  );
}
