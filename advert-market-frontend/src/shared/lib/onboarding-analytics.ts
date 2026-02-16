type OnboardingStep = 'welcome' | 'interest' | 'tour-1' | 'tour-2' | 'tour-3';
type TourTask = 'open_channel_detail' | 'approve_creative' | 'open_escrow';
type OnboardingPrimaryRole = 'advertiser' | 'owner' | 'both';
type CurrencyModeEvent = 'auto' | 'manual';

type OnboardingEventMap = {
  onboarding_view: { step: OnboardingStep };
  onboarding_primary_click: { step: OnboardingStep };
  onboarding_skip: { step: OnboardingStep };
  role_selected: { role: OnboardingPrimaryRole };
  tour_task_complete: { task: TourTask };
  onboarding_complete: { role: OnboardingPrimaryRole; variant: 'direct_replace' };
  onboarding_route_resolved: { role: OnboardingPrimaryRole; path: string };
  locale_step_shown: { source: 'welcome' | 'onboarding-first-screen' };
  locale_continue: { language: string; currencyMode: CurrencyModeEvent; currency: string };
  language_changed: { language: string };
  currency_mode_changed: { mode: CurrencyModeEvent };
  currency_changed: { currency: string };
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
