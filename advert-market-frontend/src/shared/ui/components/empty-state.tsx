import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import type { ReactNode } from 'react';
import { fadeIn, pressScale } from '../animations';

type EmptyStateProps = {
  emoji?: string;
  icon?: ReactNode;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function EmptyState({ emoji, icon, title, description, actionLabel, onAction }: EmptyStateProps) {
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
      {icon ? (
        <div
          style={{
            width: 64,
            height: 64,
            borderRadius: 20,
            background: 'var(--color-background-base)',
            border: '1px solid var(--color-border-separator)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {icon}
        </div>
      ) : emoji ? (
        <span style={{ fontSize: '48px', lineHeight: 1 }}>{emoji}</span>
      ) : null}
      <Text type="title2" weight="bold">
        {title}
      </Text>
      <Text type="body" color="secondary">
        {description}
      </Text>
      {actionLabel && onAction && (
        <div style={{ marginTop: '12px' }}>
          <motion.div {...pressScale}>
            <Button text={actionLabel} type="primary" onClick={onAction} />
          </motion.div>
        </div>
      )}
    </motion.div>
  );
}