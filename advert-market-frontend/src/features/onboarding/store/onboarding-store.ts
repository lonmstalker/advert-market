import { create } from 'zustand';

type Interest = 'advertiser' | 'owner';

type OnboardingState = {
  interests: Set<Interest>;
  tourTasksCompleted: Set<number>;
  toggleInterest: (interest: Interest) => void;
  completeTourTask: (slideIndex: number) => void;
  reset: () => void;
};

export const useOnboardingStore = create<OnboardingState>((set) => ({
  interests: new Set<Interest>(),
  tourTasksCompleted: new Set<number>(),
  toggleInterest: (interest) =>
    set((state) => {
      const next = new Set(state.interests);
      if (next.has(interest)) {
        next.delete(interest);
      } else {
        next.add(interest);
      }
      return { interests: next };
    }),
  completeTourTask: (slideIndex) =>
    set((state) => {
      const next = new Set(state.tourTasksCompleted);
      next.add(slideIndex);
      return { tourTasksCompleted: next };
    }),
  reset: () => set({ interests: new Set<Interest>(), tourTasksCompleted: new Set<number>() }),
}));
