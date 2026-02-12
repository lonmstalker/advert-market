import { useToast as useUIKitToast } from '@telegram-tools/ui-kit';

export type { ToastType } from '@telegram-tools/ui-kit';

export function useToast() {
  const { showToast } = useUIKitToast();

  return {
    showToast,
    showSuccess(message: string, duration?: number) {
      showToast(message, { type: 'success', duration });
    },
    showError(message: string, duration?: number) {
      showToast(message, { type: 'error', duration });
    },
    showInfo(message: string, duration?: number) {
      showToast(message, { type: 'info', duration });
    },
  };
}
