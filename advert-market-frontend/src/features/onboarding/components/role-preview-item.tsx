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
    <motion.div variants={itemVariant} className="flex items-center gap-2 py-1">
      <span className="flex shrink-0">{icon}</span>
      <Text type="caption1" color="secondary">
        {text}
      </Text>
    </motion.div>
  );
}
