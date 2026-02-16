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
      className="cursor-pointer bg-card-surface border border-card-border rounded-[16px] shadow-card px-3.5"
    >
      <div className="flex items-center gap-3 py-3">
        <span
          className="w-10 h-10 rounded-full flex items-center justify-center shrink-0 border border-card-border"
          style={{ background: getTransactionTypeTint(transaction.type) }}
        >
          <Icon className="w-5 h-5 text-fg-secondary" />
        </span>

        <div className="flex-1 min-w-0">
          <Text type="body" weight="medium">
            <span className="am-truncate">{t(config.i18nKey)}</span>
          </Text>
          <Text type="caption1" color="tertiary">
            {transaction.channelTitle ?? transaction.description}
          </Text>
        </div>

        <div className="shrink-0 text-right">
          <Text type="callout" weight="bold">
            <span className="am-tabnum" style={{ color: amountColor }}>{formattedAmount} TON</span>
          </Text>
          <Text type="caption1" color="tertiary">
            {time}
          </Text>
        </div>
      </div>
    </motion.div>
  );
});
