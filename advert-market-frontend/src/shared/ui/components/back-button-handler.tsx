import { backButton } from '@telegram-apps/sdk-react';
import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router';

const ROOT_PATHS = new Set(['/catalog', '/onboarding']);

export function BackButtonHandler() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const isRoot = ROOT_PATHS.has(location.pathname);

    try {
      if (!backButton.isSupported()) return;

      if (backButton.mount.isAvailable() && !backButton.isMounted()) {
        backButton.mount();
      }

      const handler = () => navigate(-1);

      if (isRoot) {
        backButton.hide.ifAvailable();
        backButton.offClick.ifAvailable(handler);
        return;
      }

      backButton.show.ifAvailable();
      backButton.onClick.ifAvailable(handler);
      return () => {
        backButton.offClick.ifAvailable(handler);
      };
    } catch {
      // Outside Telegram — graceful degradation
    }
  }, [location.pathname, navigate]);

  return null;
}
