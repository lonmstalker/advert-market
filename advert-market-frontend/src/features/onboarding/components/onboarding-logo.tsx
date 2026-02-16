import { motion, useReducedMotion } from 'motion/react';
import { MegaphoneIcon } from '@/shared/ui';

type OnboardingLogoProps = {
  testId?: string;
};

export function OnboardingLogo({ testId }: OnboardingLogoProps) {
  const shouldReduceMotion = useReducedMotion();

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
      className="am-onboarding-logo size-[120px] rounded-[32px] flex items-center justify-center relative mb-4"
      data-testid={testId}
    >
      <motion.div
        aria-hidden="true"
        initial={false}
        animate={shouldReduceMotion ? undefined : { rotate: [0, 360] }}
        transition={shouldReduceMotion ? undefined : { duration: 6, ease: 'linear', repeat: 2 }}
        className="am-onboarding-logo__orbit"
      />
      <MegaphoneIcon width={56} height={56} className="text-accent" />
    </motion.div>
  );
}
