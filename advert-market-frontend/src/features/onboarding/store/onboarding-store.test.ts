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

  it('completeTourTask adds task to tourTasksCompleted', () => {
    useOnboardingStore.getState().completeTourTask(0);
    expect(useOnboardingStore.getState().tourTasksCompleted.has(0)).toBe(true);
  });

  it('completeTourTask handles multiple tasks', () => {
    useOnboardingStore.getState().completeTourTask(0);
    useOnboardingStore.getState().completeTourTask(1);
    useOnboardingStore.getState().completeTourTask(2);
    const { tourTasksCompleted } = useOnboardingStore.getState();
    expect(tourTasksCompleted.has(0)).toBe(true);
    expect(tourTasksCompleted.has(1)).toBe(true);
    expect(tourTasksCompleted.has(2)).toBe(true);
    expect(tourTasksCompleted.size).toBe(3);
  });

  it('completeTourTask is idempotent', () => {
    useOnboardingStore.getState().completeTourTask(0);
    useOnboardingStore.getState().completeTourTask(0);
    expect(useOnboardingStore.getState().tourTasksCompleted.size).toBe(1);
  });

  it('reset clears tourTasksCompleted', () => {
    useOnboardingStore.getState().completeTourTask(0);
    useOnboardingStore.getState().completeTourTask(1);
    useOnboardingStore.getState().reset();
    expect(useOnboardingStore.getState().tourTasksCompleted.size).toBe(0);
  });
});
