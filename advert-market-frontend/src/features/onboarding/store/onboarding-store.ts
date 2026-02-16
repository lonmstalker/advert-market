import { create } from 'zustand';

type Interest = 'advertiser' | 'owner';

export type OnboardingPrimaryRole = 'advertiser' | 'owner' | 'both';
export type OnboardingTaskState = 'pending' | 'completed';
export type OnboardingResumeState = {
  interests: Interest[];
  activeSlide: number;
  completedTasks: number[];
};

const ONBOARDING_RESUME_KEY = 'am_onboarding_resume_v1';
const TOUR_SLIDE_COUNT = 3;

function readResumeState(): OnboardingResumeState | null {
  if (typeof sessionStorage === 'undefined') return null;

  const raw = sessionStorage.getItem(ONBOARDING_RESUME_KEY);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw) as Partial<OnboardingResumeState>;
    const interests = Array.isArray(parsed.interests)
      ? parsed.interests.filter((interest): interest is Interest => interest === 'advertiser' || interest === 'owner')
      : [];
    const completedTasks = Array.isArray(parsed.completedTasks)
      ? parsed.completedTasks.filter((index): index is number => Number.isInteger(index) && index >= 0)
      : [];

    const activeSlideRaw = parsed.activeSlide;
    const activeSlide =
      typeof activeSlideRaw === 'number' && Number.isInteger(activeSlideRaw)
        ? Math.max(0, Math.min(TOUR_SLIDE_COUNT - 1, activeSlideRaw))
        : 0;

    return { interests, activeSlide, completedTasks };
  } catch {
    return null;
  }
}

function persistResumeState(state: Pick<OnboardingState, 'interests' | 'activeSlide' | 'tourTasksCompleted'>): void {
  if (typeof sessionStorage === 'undefined') return;

  const payload: OnboardingResumeState = {
    interests: [...state.interests],
    activeSlide: state.activeSlide,
    completedTasks: [...state.tourTasksCompleted],
  };

  sessionStorage.setItem(ONBOARDING_RESUME_KEY, JSON.stringify(payload));
}

function clearResumeState(): void {
  if (typeof sessionStorage === 'undefined') return;
  sessionStorage.removeItem(ONBOARDING_RESUME_KEY);
}

export function resolveOnboardingPrimaryRole(interests: Iterable<Interest>): OnboardingPrimaryRole {
  const selected = new Set(interests);
  const hasAdvertiser = selected.has('advertiser');
  const hasOwner = selected.has('owner');

  if (hasAdvertiser && hasOwner) return 'both';
  if (hasOwner) return 'owner';
  return 'advertiser';
}

export function resolveOnboardingRoute(primaryRole: OnboardingPrimaryRole): '/catalog' {
  void primaryRole;
  return '/catalog';
}

type OnboardingState = {
  interests: Set<Interest>;
  activeSlide: number;
  tourTasksCompleted: Set<number>;
  getTaskState: (slideIndex: number) => OnboardingTaskState;
  getPrimaryRole: () => OnboardingPrimaryRole;
  setActiveSlide: (slideIndex: number) => void;
  toggleInterest: (interest: Interest) => void;
  completeTourTask: (slideIndex: number) => void;
  rehydrateFromSession: () => void;
  reset: () => void;
};

const initialResume = readResumeState();

export const useOnboardingStore = create<OnboardingState>((set, get) => ({
  interests: new Set<Interest>(initialResume?.interests ?? []),
  activeSlide: initialResume?.activeSlide ?? 0,
  tourTasksCompleted: new Set<number>(initialResume?.completedTasks ?? []),
  getTaskState: (slideIndex) => (get().tourTasksCompleted.has(slideIndex) ? 'completed' : 'pending'),
  getPrimaryRole: () => resolveOnboardingPrimaryRole(get().interests),
  setActiveSlide: (slideIndex) =>
    set((state) => {
      const nextSlide = Math.max(0, Math.min(TOUR_SLIDE_COUNT - 1, slideIndex));
      const nextState = { ...state, activeSlide: nextSlide };
      persistResumeState(nextState);
      return { activeSlide: nextSlide };
    }),
  toggleInterest: (interest) =>
    set((state) => {
      const next = new Set(state.interests);
      if (next.has(interest)) {
        next.delete(interest);
      } else {
        next.add(interest);
      }

      const nextState = { ...state, interests: next };
      persistResumeState(nextState);
      return { interests: next };
    }),
  completeTourTask: (slideIndex) =>
    set((state) => {
      const next = new Set(state.tourTasksCompleted);
      next.add(slideIndex);
      const nextState = { ...state, tourTasksCompleted: next };
      persistResumeState(nextState);
      return { tourTasksCompleted: next };
    }),
  rehydrateFromSession: () =>
    set(() => {
      const resume = readResumeState();
      if (!resume) {
        return {
          interests: new Set<Interest>(),
          activeSlide: 0,
          tourTasksCompleted: new Set<number>(),
        };
      }

      return {
        interests: new Set<Interest>(resume.interests),
        activeSlide: resume.activeSlide,
        tourTasksCompleted: new Set<number>(resume.completedTasks),
      };
    }),
  reset: () => {
    clearResumeState();
    set({
      interests: new Set<Interest>(),
      activeSlide: 0,
      tourTasksCompleted: new Set<number>(),
    });
  },
}));
