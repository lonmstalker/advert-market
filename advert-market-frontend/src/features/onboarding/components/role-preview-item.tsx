import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';

type RolePreviewItemProps = {
  emoji: string;
  text: string;
};

const itemVariant = {
  initial: { opacity: 0, y: 6 },
  animate: { opacity: 1, y: 0 },
};

export function RolePreviewItem({ emoji, text }: RolePreviewItemProps) {
  return (
    <motion.div
      variants={itemVariant}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        padding: '6px 0',
      }}
    >
      <span style={{ fontSize: '16px', lineHeight: 1, flexShrink: 0 }}>{emoji}</span>
      <Text type="caption1" color="secondary">
        {text}
      </Text>
    </motion.div>
  );
}
