import { motion } from 'motion/react';

const colorVars: Record<string, string> = {
  accent: 'var(--color-accent-primary)',
  warning: 'var(--color-state-warning)',
  success: 'var(--color-state-success)',
  destructive: 'var(--color-state-destructive)',
};

type PulsingDotProps = {
  color: string;
};

export function PulsingDot({ color }: PulsingDotProps) {
  const colorVar = colorVars[color] ?? 'var(--color-foreground-secondary)';
  return (
    <motion.span
      animate={{ opacity: [1, 0.4, 1] }}
      transition={{ duration: 2, repeat: Number.POSITIVE_INFINITY, ease: 'easeInOut' }}
      style={{
        width: 8,
        height: 8,
        borderRadius: '50%',
        background: colorVar,
        flexShrink: 0,
        display: 'block',
      }}
    />
  );
}
