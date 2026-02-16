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
      <div data-testid="feature-card" className="am-onboarding-feature-card">
        <div data-testid="feature-icon-box" className="am-onboarding-feature-card__icon">
          {icon}
        </div>
        <div className="am-onboarding-feature-card__copy">
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
