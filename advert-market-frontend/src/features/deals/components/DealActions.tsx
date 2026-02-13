import { Button, DialogModal } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { pressScale } from '@/shared/ui/animations';
import type { DealAction, DealActionType } from '../lib/deal-actions';

type DealActionsProps = {
  actions: DealAction[];
  onAction: (type: DealActionType) => void;
  isPending: boolean;
};

export function DealActions({ actions, onAction, isPending }: DealActionsProps) {
  const { t } = useTranslation();
  const [confirmAction, setConfirmAction] = useState<DealAction | null>(null);

  if (actions.length === 0) return null;

  const handleClick = (action: DealAction) => {
    if (action.requiresConfirm) {
      setConfirmAction(action);
    } else {
      onAction(action.type);
    }
  };

  const handleConfirm = () => {
    if (confirmAction) {
      onAction(confirmAction.type);
      setConfirmAction(null);
    }
  };

  return (
    <>
      <div
        style={{
          flexShrink: 0,
          padding: '12px 16px 32px',
          display: 'flex',
          gap: 8,
          borderTop: '0.5px solid var(--color-border-separator)',
          background: 'var(--color-background-elevated)',
        }}
      >
        {actions.map((action) => (
          <motion.div key={action.type} {...pressScale} style={{ flex: 1 }}>
            <Button
              text={t(action.i18nKey)}
              type={action.variant === 'destructive' ? 'secondary' : action.variant}
              onClick={() => handleClick(action)}
              loading={isPending}
              style={
                action.variant === 'destructive'
                  ? { color: 'var(--color-destructive)' }
                  : undefined
              }
            />
          </motion.div>
        ))}
      </div>

      <DialogModal
        active={!!confirmAction}
        title={confirmAction ? t('deals.confirm.title') : ''}
        description={confirmAction ? t(`deals.confirm.${confirmAction.type}`) : ''}
        confirmText={confirmAction ? t(confirmAction.i18nKey) : ''}
        closeText={t('common.cancel')}
        onDelete={confirmAction?.variant === 'destructive' ? handleConfirm : undefined}
        onConfirm={confirmAction?.variant !== 'destructive' ? handleConfirm : undefined}
        onClose={() => setConfirmAction(null)}
      />
    </>
  );
}
