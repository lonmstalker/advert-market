import { easeInOut } from 'motion';

const linearEase = (t: number) => t;

const motionSafe = window.matchMedia?.('(prefers-reduced-motion: no-preference)').matches ?? true;

const noMotion = {
  initial: {},
  animate: {},
  exit: {},
  transition: { duration: 0 },
};

function safe<T extends Record<string, unknown>>(preset: T): T {
  return motionSafe ? preset : (noMotion as unknown as T);
}

/* Apple-style cubic bezier curves */
const appleEaseOut = [0.25, 1, 0.5, 1] as const;
const appleEaseInOut = [0.45, 0, 0.55, 1] as const;
const sheetEase = [0.32, 0.72, 0, 1] as const;

/* Natural spring configs — feel like real physical objects */
const snappySpring = { type: 'spring' as const, stiffness: 500, damping: 30, mass: 0.8 };
const gentleSpring = { type: 'spring' as const, stiffness: 300, damping: 24, mass: 1 };

export const fadeIn = safe({
  initial: { opacity: 0 },
  animate: { opacity: 1 },
  exit: { opacity: 0 },
  transition: { duration: 0.22, ease: appleEaseOut },
} as const);

export const slideUp = safe({
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: 10 },
  transition: { ...gentleSpring },
} as const);

export const slideDown = safe({
  initial: { opacity: 0, y: -12 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -8 },
  transition: { duration: 0.22, ease: appleEaseOut },
} as const);

export const scaleIn = safe({
  initial: { opacity: 0, scale: 0.92 },
  animate: { opacity: 1, scale: 1 },
  exit: { opacity: 0, scale: 0.95 },
  transition: { ...snappySpring },
} as const);

export const slideFromRight = safe({
  initial: { opacity: 0, x: 24 },
  animate: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: 16 },
  transition: { ...gentleSpring },
} as const);

export const slideFromLeft = safe({
  initial: { opacity: 0, x: -24 },
  animate: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: -16 },
  transition: { ...gentleSpring },
} as const);

export const slideFromBottom = safe({
  initial: { y: '100%' },
  animate: { y: 0 },
  exit: { y: '100%' },
  transition: { duration: 0.32, ease: sheetEase },
} as const);

export const staggerChildren = motionSafe
  ? ({
      variants: {
        initial: {},
        animate: { transition: { staggerChildren: 0.05, delayChildren: 0.02 } },
      },
      initial: 'initial',
      animate: 'animate',
    } as const)
  : ({
      variants: { initial: {}, animate: {} },
      initial: 'initial',
      animate: 'animate',
    } as const);

export const listItem = safe({
  initial: { opacity: 0, x: -8 },
  animate: { opacity: 1, x: 0 },
  transition: { duration: 0.2, ease: appleEaseOut },
} as const);

export const tapScale = motionSafe
  ? ({ whileTap: { scale: 0.97 }, transition: { ...snappySpring } } as const)
  : ({ whileTap: {}, transition: { duration: 0 } } as const);

export const pressScale = motionSafe
  ? ({
      whileTap: { scale: 0.96 },
      transition: { type: 'spring' as const, stiffness: 450, damping: 22, mass: 0.7 },
    } as const)
  : ({ whileTap: {}, transition: { duration: 0 } } as const);

export const shimmer = safe({
  initial: { backgroundPosition: '-200% 0' },
  animate: { backgroundPosition: '200% 0' },
  transition: { duration: 1.5, repeat: Number.POSITIVE_INFINITY, ease: linearEase },
} as const);

export const pulse = safe({
  animate: { opacity: [1, 0.5, 1] },
  transition: { duration: 1.5, repeat: Number.POSITIVE_INFINITY, ease: easeInOut },
});

export const toast = safe({
  initial: { opacity: 0, y: -30, scale: 0.96 },
  animate: { opacity: 1, y: 0, scale: 1 },
  exit: { opacity: 0, y: -20, scale: 0.97 },
  transition: { ...snappySpring },
} as const);

/** Subtle card hover lift — translateY -1px with shadow hint. */
export const cardHover = motionSafe
  ? ({ whileHover: { y: -1 }, transition: { duration: 0.2, ease: appleEaseInOut } } as const)
  : ({ whileHover: {}, transition: { duration: 0 } } as const);

/** Error shake — 3-oscillation horizontal shake for invalid input. */
export const errorShake = safe({
  animate: { x: [0, -6, 5, -4, 3, -1, 0] },
  transition: { duration: 0.4, ease: appleEaseOut },
} as const);

/** Smooth focus ring scale-in for custom focusable elements. */
export const focusRing = safe({
  initial: { opacity: 0, scale: 0.95 },
  animate: { opacity: 1, scale: 1 },
  exit: { opacity: 0, scale: 0.95 },
  transition: { duration: 0.15, ease: appleEaseOut },
} as const);
