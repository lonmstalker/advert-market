import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import type { ReactNode } from 'react';
import { pressScale, scaleIn } from '../animations';

type EmptyStateProps = {
  icon?: ReactNode;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function EmptyState({ icon, title, description, actionLabel, onAction }: EmptyStateProps) {
  return (
    <motion.div {...scaleIn} className="am-empty-state">
      {icon && <div className="am-empty-state__icon">{icon}</div>}
      <Text type="title2" weight="bold">
        {title}
      </Text>
      <Text type="body" color="secondary">
        {description}
      </Text>
      {actionLabel && onAction && (
        <div className="am-empty-state__action">
          <motion.div {...pressScale}>
            <Button text={actionLabel} type="primary" onClick={onAction} />
          </motion.div>
        </div>
      )}
    </motion.div>
  );
}
