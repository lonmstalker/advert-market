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
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
          padding: '18px 18px',
          borderRadius: 16,
          backgroundColor: 'var(--color-background-base)',
          border: '1px solid var(--color-border-separator)',
          cursor: 'default',
        }}
      >
        <div
          data-testid="feature-icon-box"
          style={{
            width: 56,
            height: 56,
            borderRadius: 16,
            backgroundColor: 'var(--am-soft-accent-bg)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
            color: 'var(--color-accent-primary)',
          }}
        >
          {icon}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
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
