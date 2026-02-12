import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { fadeIn } from '../animations';

type EmptyStateProps = {
  emoji: string;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function EmptyState({ emoji, title, description, actionLabel, onAction }: EmptyStateProps) {
  return (
    <motion.div
      {...fadeIn}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '48px 24px',
        gap: '12px',
      }}
    >
      <span style={{ fontSize: '48px', lineHeight: 1 }}>{emoji}</span>
      <Text type="title2" weight="bold">
        {title}
      </Text>
      <Text type="body" color="secondary">
        {description}
      </Text>
      {actionLabel && onAction && (
        <Button text={actionLabel} type="primary" onClick={onAction} style={{ marginTop: '12px' }} />
      )}
    </motion.div>
  );
}
