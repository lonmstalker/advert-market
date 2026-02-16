import type { OnboardingPrimaryRole } from '@/features/onboarding/store/onboarding-store';

type OnboardingStep = 'welcome' | 'interest' | 'tour-1' | 'tour-2' | 'tour-3';
type TourTask = 'open_channel_detail' | 'approve_creative' | 'open_escrow';

type OnboardingEventMap = {
  onboarding_view: { step: OnboardingStep };
  onboarding_primary_click: { step: OnboardingStep };
  onboarding_skip: { step: OnboardingStep };
  role_selected: { role: OnboardingPrimaryRole };
  tour_task_complete: { task: TourTask };
  onboarding_complete: { role: OnboardingPrimaryRole; variant: 'direct_replace' };
};

type OnboardingEventName = keyof OnboardingEventMap;
type OnboardingTransport = <K extends OnboardingEventName>(event: K, payload: OnboardingEventMap[K]) => void;

let transport: OnboardingTransport = () => {
  // No-op by default. Runtime integrators can attach transport later.
};

export function setOnboardingAnalyticsTransport(nextTransport: OnboardingTransport): void {
  transport = nextTransport;
}

export function trackOnboardingEvent<K extends OnboardingEventName>(event: K, payload: OnboardingEventMap[K]): void {
  transport(event, payload);

  if (import.meta.env.DEV) {
    // Dev diagnostics without polluting production logging.
    console.info('[onboarding-event]', event, payload);
  }
}
