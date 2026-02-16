import { motion } from 'motion/react';

const colorClasses: Record<string, string> = {
  accent: 'bg-accent',
  warning: 'bg-warning',
  success: 'bg-success',
  destructive: 'bg-destructive',
};

type PulsingDotProps = {
  color: string;
};

export function PulsingDot({ color }: PulsingDotProps) {
  const bg = colorClasses[color] ?? 'bg-fg-secondary';
  return (
    <motion.span
      animate={{ opacity: [1, 0.4, 1] }}
      transition={{ duration: 2, repeat: Number.POSITIVE_INFINITY, ease: 'easeInOut' }}
      className={`block h-2 w-2 shrink-0 rounded-full ${bg}`}
    />
  );
}
