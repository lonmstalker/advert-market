import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';

type TaskHintProps = {
  text: string;
};

export function TaskHint({ text }: TaskHintProps) {
  return (
    <motion.div
      animate={{ opacity: [1, 0.5, 1] }}
      transition={{ duration: 1.5, repeat: Number.POSITIVE_INFINITY, ease: 'easeInOut' }}
      style={{
        textAlign: 'center',
        padding: '12px 0',
      }}
    >
      <Text type="caption1" color="accent">
        {text}
      </Text>
    </motion.div>
  );
}
