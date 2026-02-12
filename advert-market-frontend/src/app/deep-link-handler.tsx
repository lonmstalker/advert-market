import { useEffect } from 'react';
import { useNavigate } from 'react-router';

export function DeepLinkHandler() {
  const navigate = useNavigate();

  useEffect(() => {
    try {
      const tg = window.Telegram?.WebApp;
      const startParam = tg?.initDataUnsafe?.start_param;
      if (!startParam) return;

      if (startParam.startsWith('channel_')) {
        navigate(`/catalog/${startParam.replace('channel_', '')}`, { replace: true });
      } else if (startParam.startsWith('deal_')) {
        navigate(`/deals/${startParam.replace('deal_', '')}`, { replace: true });
      }
    } catch {
      // Outside Telegram — no deep links
    }
  }, [navigate]);

  return null;
}
