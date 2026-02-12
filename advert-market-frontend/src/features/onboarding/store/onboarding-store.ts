import { create } from 'zustand';

type Interest = 'advertiser' | 'owner';

type OnboardingState = {
  interests: Set<Interest>;
  toggleInterest: (interest: Interest) => void;
  reset: () => void;
};

export const useOnboardingStore = create<OnboardingState>((set) => ({
  interests: new Set<Interest>(),
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
  reset: () => set({ interests: new Set<Interest>() }),
}));
