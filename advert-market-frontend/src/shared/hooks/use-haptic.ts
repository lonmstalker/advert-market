import {
  hapticFeedbackImpactOccurred,
  hapticFeedbackNotificationOccurred,
  hapticFeedbackSelectionChanged,
  isHapticFeedbackSupported,
} from '@telegram-apps/sdk-react';

type ImpactStyle = 'light' | 'medium' | 'heavy' | 'rigid' | 'soft';
type NotificationType = 'error' | 'success' | 'warning';

export type HapticFeedback = {
  impactOccurred: (style: ImpactStyle) => void;
  notificationOccurred: (type: NotificationType) => void;
  selectionChanged: () => void;
};

function isSupported(): boolean {
  try {
    return isHapticFeedbackSupported();
  } catch {
    return false;
  }
}

export function useHaptic(): HapticFeedback {
  return {
    impactOccurred(style: ImpactStyle) {
      if (isSupported()) {
        try {
          hapticFeedbackImpactOccurred(style);
        } catch {
          /* no-op outside TG */
        }
      }
    },
    notificationOccurred(type: NotificationType) {
      if (isSupported()) {
        try {
          hapticFeedbackNotificationOccurred(type);
        } catch {
          /* no-op outside TG */
        }
      }
    },
    selectionChanged() {
      if (isSupported()) {
        try {
          hapticFeedbackSelectionChanged();
        } catch {
          /* no-op outside TG */
        }
      }
    },
  };
}
