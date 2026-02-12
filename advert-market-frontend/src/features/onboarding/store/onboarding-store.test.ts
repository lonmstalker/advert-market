import { useOnboardingStore } from './onboarding-store';

describe('useOnboardingStore', () => {
  beforeEach(() => {
    useOnboardingStore.getState().reset();
  });

  it('has empty interests initially', () => {
    expect(useOnboardingStore.getState().interests.size).toBe(0);
  });

  it('adds interest on toggleInterest', () => {
    useOnboardingStore.getState().toggleInterest('advertiser');
    expect(useOnboardingStore.getState().interests.has('advertiser')).toBe(true);
  });

  it('removes interest on second toggleInterest', () => {
    useOnboardingStore.getState().toggleInterest('advertiser');
    useOnboardingStore.getState().toggleInterest('advertiser');
    expect(useOnboardingStore.getState().interests.has('advertiser')).toBe(false);
  });

  it('toggles both interests independently', () => {
    useOnboardingStore.getState().toggleInterest('advertiser');
    useOnboardingStore.getState().toggleInterest('owner');
    const { interests } = useOnboardingStore.getState();
    expect(interests.has('advertiser')).toBe(true);
    expect(interests.has('owner')).toBe(true);
  });

  it('resets interests to empty', () => {
    useOnboardingStore.getState().toggleInterest('advertiser');
    useOnboardingStore.getState().reset();
    expect(useOnboardingStore.getState().interests.size).toBe(0);
  });

  it('resets after selecting both interests', () => {
    useOnboardingStore.getState().toggleInterest('advertiser');
    useOnboardingStore.getState().toggleInterest('owner');
    useOnboardingStore.getState().reset();
    expect(useOnboardingStore.getState().interests.size).toBe(0);
  });
});
