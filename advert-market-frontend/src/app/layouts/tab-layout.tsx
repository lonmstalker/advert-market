import { Outlet } from 'react-router';
import { BackButtonHandler, BottomTabs } from '@/shared/ui';

export function TabLayout() {
  return (
    <>
      <BackButtonHandler />
      <div style={{ paddingBottom: 'calc(var(--am-bottom-tabs-height) + var(--am-safe-area-bottom) + 20px)' }}>
        <Outlet />
      </div>
      <BottomTabs />
    </>
  );
}
