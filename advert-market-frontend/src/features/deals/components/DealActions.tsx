import { Button, DialogModal } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { FixedBottomBar } from '@/shared/ui';
import { pressScale } from '@/shared/ui/animations';
import type { DealAction, DealActionType } from '../lib/deal-actions';

type DealActionsProps = {
  actions: DealAction[];
  onAction: (type: DealActionType) => void;
  isPending: boolean;
};

export function DealActions({ actions, onAction, isPending }: DealActionsProps) {
  const { t } = useTranslation();
  const haptic = useHaptic();
  const [confirmAction, setConfirmAction] = useState<DealAction | null>(null);

  if (actions.length === 0) return null;

  const handleClick = (action: DealAction) => {
    if (action.requiresConfirm) {
      setConfirmAction(action);
    } else {
      haptic.notificationOccurred('success');
      onAction(action.type);
    }
  };

  const handleConfirm = () => {
    if (confirmAction) {
      haptic.notificationOccurred('success');
      onAction(confirmAction.type);
      setConfirmAction(null);
    }
  };

  return (
    <>
      <FixedBottomBar style={{ display: 'flex', gap: 8 }}>
        {actions.map((action) => (
          <motion.div key={action.type} {...pressScale} style={{ flex: 1 }}>
            <Button
              text={t(action.i18nKey)}
              type={action.variant === 'destructive' ? 'secondary' : action.variant}
              onClick={() => handleClick(action)}
              loading={isPending}
              className={action.variant === 'destructive' ? 'am-button-destructive' : undefined}
            />
          </motion.div>
        ))}
      </FixedBottomBar>

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
