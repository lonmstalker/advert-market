import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { memo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { formatTonCompact } from '@/shared/lib/ton-format';
import { listItem, pressScale } from '@/shared/ui/animations';
import {
  formatAmountWithSign,
  getAmountColor,
  getTransactionTypeConfig,
  getTransactionTypeTint,
} from '../lib/transaction-type';
import type { Transaction } from '../types/wallet';

type TransactionListItemProps = {
  transaction: Transaction;
  onClick: () => void;
};

export const TransactionListItem = memo(function TransactionListItem({
  transaction,
  onClick,
}: TransactionListItemProps) {
  const { t } = useTranslation();
  const haptic = useHaptic();
  const config = getTransactionTypeConfig(transaction.type);
  const amountColor = getAmountColor(transaction.type, transaction.direction);
  const formattedAmount = formatAmountWithSign(formatTonCompact(transaction.amountNano), transaction.direction);

  const time = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' }).format(
    new Date(transaction.createdAt),
  );

  const Icon = config.Icon;

  const handleClick = useCallback(() => {
    haptic.impactOccurred('light');
    onClick();
  }, [haptic, onClick]);

  return (
    <motion.div
      {...listItem}
      {...pressScale}
      onClick={handleClick}
      style={{
        cursor: 'pointer',
        background: 'color-mix(in srgb, var(--am-card-surface) 94%, transparent)',
        border: '1px solid var(--am-card-border)',
        borderRadius: 16,
        boxShadow: 'var(--am-card-shadow)',
        padding: '0 14px',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '12px 0',
        }}
      >
        <span
          style={{
            width: 40,
            height: 40,
            borderRadius: '50%',
            background: getTransactionTypeTint(transaction.type),
            border: '1px solid color-mix(in srgb, var(--am-card-border) 90%, transparent)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <Icon style={{ width: 20, height: 20, color: 'var(--color-foreground-secondary)' }} />
        </span>

        <div style={{ flex: 1, minWidth: 0 }}>
          <Text type="body" weight="medium">
            <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {t(config.i18nKey)}
            </span>
          </Text>
          <Text type="caption1" color="tertiary">
            {transaction.channelTitle ?? transaction.description}
          </Text>
        </div>

        <div style={{ flexShrink: 0, textAlign: 'right' }}>
          <Text type="callout" weight="bold">
            <span style={{ fontVariantNumeric: 'tabular-nums', color: amountColor }}>{formattedAmount} TON</span>
          </Text>
          <Text type="caption1" color="tertiary">
            {time}
          </Text>
        </div>
      </div>
    </motion.div>
  );
});
