import { retrieveLaunchParams } from '@telegram-apps/sdk-react';
import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router';

export function DeepLinkHandler() {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    // Deep links are a launch-time concern. Avoid overriding in-app navigation or refreshes
    // on nested routes (e.g. /profile/channels/new).
    if (location.pathname !== '/' && location.pathname !== '/catalog') return;

    try {
      const lp = retrieveLaunchParams(true);
      const startParam = lp.tgWebAppStartParam ?? lp.tgWebAppData?.startParam;
      if (typeof startParam !== 'string' || startParam.length === 0) return;

      if (startParam.startsWith('channel_')) {
        navigate(`/catalog/channels/${startParam.replace('channel_', '')}`, { replace: true });
      } else if (startParam.startsWith('deal_')) {
        navigate(`/deals/${startParam.replace('deal_', '')}`, { replace: true });
      }
    } catch {
      // Outside Telegram — no deep links
    }
  }, [location.pathname, navigate]);

  return null;
}
