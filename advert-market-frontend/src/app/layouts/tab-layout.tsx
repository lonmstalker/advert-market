import { Outlet } from 'react-router';
import { BackButtonHandler, BottomTabs } from '@/shared/ui';

export function TabLayout() {
  return (
    <>
      <BackButtonHandler />
      <div className="am-tab-layout">
        <Outlet />
      </div>
      <BottomTabs />
    </>
  );
}
