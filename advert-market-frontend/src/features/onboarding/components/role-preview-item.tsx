import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import type { ReactNode } from 'react';

type RolePreviewItemProps = {
  icon: ReactNode;
  text: string;
};

const itemVariant = {
  initial: { opacity: 0, y: 6 },
  animate: { opacity: 1, y: 0 },
};

export function RolePreviewItem({ icon, text }: RolePreviewItemProps) {
  return (
    <motion.div
      variants={itemVariant}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        padding: '4px 0',
      }}
    >
      <span style={{ display: 'flex', flexShrink: 0 }}>{icon}</span>
      <Text type="caption1" color="secondary">
        {text}
      </Text>
    </motion.div>
  );
}
