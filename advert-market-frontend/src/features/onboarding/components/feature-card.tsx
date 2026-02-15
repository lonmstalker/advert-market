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
          gap: '16px',
          padding: '20px',
          borderRadius: '12px',
          backgroundColor: 'var(--color-background-secondary)',
          cursor: 'default',
        }}
      >
        <div
          style={{
            width: '48px',
            height: '48px',
            borderRadius: '12px',
            backgroundColor: 'color-mix(in srgb, var(--color-accent-primary) 10%, transparent)',
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
          <Text type="body" weight="medium">
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
