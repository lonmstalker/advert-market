import { useToast as useUIKitToast } from '@telegram-tools/ui-kit';
import { useMemo } from 'react';

export type { ToastType } from '@telegram-tools/ui-kit';

export function useToast() {
  const { showToast } = useUIKitToast();

  return useMemo(
    () => ({
      showToast,
      showSuccess(message: string, duration?: number) {
        showToast(message, { type: 'success', ...(duration != null ? { duration } : {}) });
      },
      showError(message: string, duration?: number) {
        showToast(message, { type: 'error', ...(duration != null ? { duration } : {}) });
      },
      showInfo(message: string, duration?: number) {
        showToast(message, { type: 'info', ...(duration != null ? { duration } : {}) });
      },
    }),
    [showToast],
  );
}
