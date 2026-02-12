import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router';

const ROOT_PATHS = new Set(['/catalog', '/onboarding']);

export function BackButtonHandler() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const isRoot = ROOT_PATHS.has(location.pathname);

    try {
      const tg = window.Telegram?.WebApp;
      if (!tg) return;

      if (isRoot) {
        tg.BackButton?.hide();
      } else {
        tg.BackButton?.show();
        const handler = () => navigate(-1);
        tg.BackButton?.onClick(handler);
        return () => {
          tg.BackButton?.offClick(handler);
        };
      }
    } catch {
      // Outside Telegram — graceful degradation
    }
  }, [location.pathname, navigate]);

  return null;
}
