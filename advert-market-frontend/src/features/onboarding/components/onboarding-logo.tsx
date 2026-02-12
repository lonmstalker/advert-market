import { motion } from 'motion/react';

export function OnboardingLogo() {
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
      <div
        style={{
          position: 'absolute',
          inset: '-4px',
          borderRadius: '36px',
          border: '1px solid var(--color-accent-primary)',
          opacity: 0.4,
          animation: 'logo-ring-rotate 8s linear infinite',
          backgroundImage:
            'conic-gradient(from 0deg, var(--color-accent-primary) 0%, transparent 30%, transparent 70%, var(--color-accent-primary) 100%)',
          mask: 'linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)',
          maskComposite: 'exclude',
          WebkitMask: 'linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)',
          WebkitMaskComposite: 'xor',
          padding: '1px',
        }}
      />
      <span style={{ fontSize: '56px', lineHeight: 1 }}>📢</span>
      <style>
        {`@keyframes logo-ring-rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}
      </style>
    </motion.div>
  );
}
