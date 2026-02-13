import { Button, Input, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { formatTon } from '@/shared/lib/ton-format';
import { pressScale } from '@/shared/ui/animations';
import { TextareaField } from '@/shared/ui/components/textarea-field';
import { useNegotiateContext } from './NegotiateContext';

export function NegotiateSheetContent() {
  const { t } = useTranslation();
  const [price, setPrice] = useState('');
  const [message, setMessage] = useState('');

  const { currentPriceNano, onSubmit, isPending } = useNegotiateContext();

  const handleSubmit = () => {
    const priceNum = Number.parseFloat(price);
    if (Number.isNaN(priceNum) || priceNum <= 0) return;
    const priceNano = Math.round(priceNum * 1_000_000_000);
    onSubmit(priceNano, message.trim() || undefined);
  };

  const isValid = Number.parseFloat(price) > 0;

  return (
    <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Text type="title2" weight="bold">
        {t('deals.negotiate.title')}
      </Text>

      <div>
        <Text type="subheadline2" color="secondary" style={{ marginBottom: 8 }}>
          {t('deals.negotiate.currentPrice')}
        </Text>
        <Text type="body" weight="medium">
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(currentPriceNano)}</span>
        </Text>
      </div>

      <div>
        <Text type="subheadline2" color="secondary" style={{ marginBottom: 8 }}>
          {t('deals.negotiate.proposedPrice')}
        </Text>
        <Input type="number" value={price} onChange={(v) => setPrice(v)} placeholder="0.00" />
      </div>

      <TextareaField
        value={message}
        onChange={setMessage}
        label={t('deals.negotiate.message')}
        placeholder={t('deals.negotiate.messagePlaceholder')}
        maxLength={500}
        rows={3}
      />

      <motion.div {...pressScale}>
        <Button
          text={t('deals.negotiate.submit')}
          type="primary"
          onClick={handleSubmit}
          disabled={!isValid}
          loading={isPending}
        />
      </motion.div>
    </div>
  );
}
