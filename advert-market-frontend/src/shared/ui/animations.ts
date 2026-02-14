import { easeInOut, easeOut } from 'motion';

const linearEase = (t: number) => t;

export const fadeIn = {
  initial: { opacity: 0 },
  animate: { opacity: 1 },
  exit: { opacity: 0 },
  transition: { duration: 0.2 },
} as const;

export const slideUp = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: 20 },
  transition: { duration: 0.25, ease: easeOut },
} as const;

export const slideDown = {
  initial: { opacity: 0, y: -20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
  transition: { duration: 0.25, ease: easeOut },
} as const;

export const scaleIn = {
  initial: { opacity: 0, scale: 0.9 },
  animate: { opacity: 1, scale: 1 },
  exit: { opacity: 0, scale: 0.9 },
  transition: { duration: 0.2, ease: easeOut },
} as const;

export const slideFromRight = {
  initial: { opacity: 0, x: 30 },
  animate: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: 30 },
  transition: { duration: 0.25, ease: easeOut },
} as const;

export const slideFromBottom = {
  initial: { y: '100%' },
  animate: { y: 0 },
  exit: { y: '100%' },
  transition: { duration: 0.3, ease: [0.32, 0.72, 0, 1] },
} as const;

export const staggerChildren = {
  animate: { transition: { staggerChildren: 0.05 } },
} as const;

export const listItem = {
  initial: { opacity: 0, x: -10 },
  animate: { opacity: 1, x: 0 },
  transition: { duration: 0.2 },
} as const;

export const tapScale = {
  whileTap: { scale: 0.97 },
  transition: { duration: 0.1 },
} as const;

export const pressScale = {
  whileTap: { scale: 0.95 },
  transition: { type: 'spring' as const, stiffness: 400, damping: 17 },
} as const;

export const shimmer = {
  initial: { backgroundPosition: '-200% 0' },
  animate: { backgroundPosition: '200% 0' },
  transition: { duration: 1.5, repeat: Number.POSITIVE_INFINITY, ease: linearEase },
} as const;

export const pulse = {
  animate: { opacity: [1, 0.5, 1] },
  transition: { duration: 1.5, repeat: Number.POSITIVE_INFINITY, ease: easeInOut },
} as const;

export const toast = {
  initial: { opacity: 0, y: -40, scale: 0.95 },
  animate: { opacity: 1, y: 0, scale: 1 },
  exit: { opacity: 0, y: -40, scale: 0.95 },
  transition: { duration: 0.25, ease: [0.32, 0.72, 0, 1] },
} as const;
