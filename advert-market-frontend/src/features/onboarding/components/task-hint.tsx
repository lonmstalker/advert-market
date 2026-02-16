import { Text } from '@telegram-tools/ui-kit';
import { motion, useReducedMotion } from 'motion/react';

type TaskHintProps = {
  text: string;
};

export function TaskHint({ text }: TaskHintProps) {
  const shouldReduceMotion = useReducedMotion();

  return (
    <motion.div
      data-testid="onboarding-task-hint"
      className="am-onboarding-task-hint"
      animate={shouldReduceMotion ? undefined : { opacity: [1, 0.6, 1] }}
      transition={shouldReduceMotion ? undefined : { duration: 1.2, repeat: 2, ease: 'easeInOut' }}
    >
      <Text type="caption1" color="accent">
        {text}
      </Text>
    </motion.div>
  );
}
