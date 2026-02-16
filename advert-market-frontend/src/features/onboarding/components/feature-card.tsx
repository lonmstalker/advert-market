import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import type { ReactNode } from 'react';

type FeatureCardProps = {
  icon: ReactNode;
  title: string;
  hint: string;
};

const itemVariant = {
  initial: { opacity: 0, x: -10 },
  animate: { opacity: 1, x: 0 },
  transition: { duration: 0.2 },
};

export function FeatureCard({ icon, title, hint }: FeatureCardProps) {
  return (
    <motion.div {...itemVariant}>
      <div
        data-testid="feature-card"
        className="flex items-center gap-4 p-[18px] rounded-card bg-bg-base border border-separator cursor-default"
      >
        <div
          data-testid="feature-icon-box"
          className="size-14 rounded-[16px] bg-soft-accent flex items-center justify-center shrink-0 text-accent"
        >
          {icon}
        </div>
        <div className="flex-1 min-w-0">
          <Text type="body" weight="bold">
            {title}
          </Text>
          <Text type="subheadline2" color="secondary">
            {hint}
          </Text>
        </div>
      </div>
    </motion.div>
  );
}
