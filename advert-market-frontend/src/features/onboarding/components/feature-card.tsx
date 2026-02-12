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
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '14px',
          padding: '14px 16px',
          borderRadius: '12px',
          backgroundColor: 'var(--color-background-secondary)',
          cursor: 'default',
        }}
      >
        <div
          style={{
            width: '40px',
            height: '40px',
            borderRadius: '10px',
            backgroundColor: 'rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.1)',
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
          <Text type="subheadline1" weight="medium">
            {title}
          </Text>
          <Text type="caption1" color="secondary">
            {hint}
          </Text>
        </div>
      </div>
    </motion.div>
  );
}
