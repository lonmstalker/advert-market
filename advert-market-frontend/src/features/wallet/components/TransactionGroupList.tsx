import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { staggerChildren } from '@/shared/ui/animations';
import { groupByDay } from '../lib/day-group';
import type { Transaction } from '../types/wallet';
import { TransactionListItem } from './TransactionListItem';

type TransactionGroupListProps = {
  transactions: Transaction[];
  onItemClick: (txId: string) => void;
};

export function TransactionGroupList({ transactions, onItemClick }: TransactionGroupListProps) {
  const { t, i18n } = useTranslation();

  const groups = useMemo(() => groupByDay(transactions, i18n.language, t), [transactions, i18n.language, t]);

  return (
    <motion.div
      {...staggerChildren}
      initial="initial"
      animate="animate"
      className="flex flex-col gap-2.5"
    >
      {groups.map((group) => (
        <div key={group.date}>
          <div className="pt-1 px-0.5 pb-1.5">
            <Text type="caption1" weight="bold" color="secondary">
              {group.label}
            </Text>
          </div>
          {group.transactions.map((tx) => (
            <TransactionListItem key={tx.id} transaction={tx} onClick={() => onItemClick(tx.id)} />
          ))}
        </div>
      ))}
    </motion.div>
  );
}
