import { Sheet } from '@telegram-tools/ui-kit';
import { useEffect } from 'react';
import { trackOnboardingEvent } from '@/shared/lib/onboarding-analytics';
import { LocaleCurrencyEditor } from '@/shared/ui';

type LocaleCurrencyStepSheetProps = {
  open: boolean;
  onClose: () => void;
  onContinue: () => void;
};

export function LocaleCurrencyStepSheet({ open, onClose, onContinue }: LocaleCurrencyStepSheetProps) {
  useEffect(() => {
    if (open) {
      trackOnboardingEvent('locale_step_shown', {
        source: 'welcome',
      });
    }
  }, [open]);

  const StepSheet = () => (
    <LocaleCurrencyEditor
      mode="onboarding"
      onContinue={() => {
        onClose();
        onContinue();
      }}
    />
  );

  return <Sheet sheets={{ localeCurrency: StepSheet }} activeSheet="localeCurrency" opened={open} onClose={onClose} />;
}
