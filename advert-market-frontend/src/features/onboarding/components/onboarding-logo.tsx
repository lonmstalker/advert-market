import { motion, useReducedMotion } from 'motion/react';
import { MegaphoneIcon } from '@/shared/ui';

export function OnboardingLogo() {
  const shouldReduceMotion = useReducedMotion();

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
      style={{
        width: '120px',
        height: '120px',
        borderRadius: '32px',
        backgroundColor: 'var(--color-background-base)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        marginBottom: '16px',
      }}
    >
      <motion.div
        aria-hidden="true"
        initial={false}
        animate={shouldReduceMotion ? undefined : { rotate: [0, 360] }}
        transition={shouldReduceMotion ? undefined : { duration: 6, ease: 'linear', repeat: 2 }}
        style={{
          position: 'absolute',
          inset: '-4px',
          borderRadius: '36px',
          border: '1px solid var(--color-accent-primary)',
          opacity: 0.4,
          backgroundImage:
            'conic-gradient(from 0deg, var(--color-accent-primary) 0%, transparent 30%, transparent 70%, var(--color-accent-primary) 100%)',
          mask: 'linear-gradient(var(--color-static-white) 0 0) content-box, linear-gradient(var(--color-static-white) 0 0)',
          maskComposite: 'exclude',
          WebkitMask:
            'linear-gradient(var(--color-static-white) 0 0) content-box, linear-gradient(var(--color-static-white) 0 0)',
          WebkitMaskComposite: 'xor',
          padding: '1px',
        }}
      />
      <MegaphoneIcon width={56} height={56} style={{ color: 'var(--color-accent-primary)' }} />
    </motion.div>
  );
}
