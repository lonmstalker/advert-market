import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import type { TransactionStatus } from '../types/wallet';

type StatusConfig = {
  badgeClass: string;
  textColor: 'accent' | 'danger';
  i18nKey: string;
};

const STATUS_CONFIG: Record<TransactionStatus, StatusConfig> = {
  pending: {
    badgeClass: 'bg-soft-warning',
    textColor: 'accent',
    i18nKey: 'wallet.status.pending',
  },
  confirmed: {
    badgeClass: 'bg-soft-success',
    textColor: 'accent',
    i18nKey: 'wallet.status.confirmed',
  },
  failed: {
    badgeClass: 'bg-soft-destructive',
    textColor: 'danger',
    i18nKey: 'wallet.status.failed',
  },
};

type TransactionStatusBadgeProps = {
  status: TransactionStatus;
};

export function TransactionStatusBadge({ status }: TransactionStatusBadgeProps) {
  const { t } = useTranslation();
  const config = STATUS_CONFIG[status];

  return (
    <span
      data-testid="transaction-status-badge"
      className={`inline-block rounded-[6px] px-2 py-0.5 whitespace-nowrap ${config.badgeClass}`}
    >
      <Text type="caption1" weight="bold" color={config.textColor}>
        {t(config.i18nKey)}
      </Text>
    </span>
  );
}
