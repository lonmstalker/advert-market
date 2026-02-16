import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { pressScale } from '@/shared/ui/animations';
import { TextareaField } from '@/shared/ui/components/textarea-field';
import { useNegotiateContext } from './NegotiateContext';

export function NegotiateSheetContent() {
  const { t } = useTranslation();
  const haptic = useHaptic();
  const [reason, setReason] = useState('');

  const { actionLabelKey, reasonRequired, onSubmit, isPending } = useNegotiateContext();

  const trimmedReason = reason.trim();
  const canSubmit = reasonRequired ? trimmedReason.length > 0 : true;

  const handleSubmit = () => {
    if (!canSubmit) return;
    haptic.impactOccurred('medium');
    onSubmit(trimmedReason || undefined);
  };

  return (
    <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Text type="title2" weight="bold">
        {t('deals.negotiate.title')}
      </Text>

      <TextareaField
        value={reason}
        onChange={setReason}
        label={t('deals.negotiate.message')}
        placeholder={t('deals.negotiate.messagePlaceholder')}
        maxLength={500}
        rows={4}
      />

      <motion.div {...pressScale}>
        <Button
          text={t(actionLabelKey)}
          type="primary"
          onClick={handleSubmit}
          disabled={!canSubmit}
          loading={isPending}
        />
      </motion.div>
    </div>
  );
}
