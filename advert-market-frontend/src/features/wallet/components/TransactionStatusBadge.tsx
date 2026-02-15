import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import type { TransactionStatus } from '../types/wallet';

type StatusStyle = {
  colorVar: string;
  bg: string;
  i18nKey: string;
};

// Using rgba for background tints — same pattern as DealStatusBadge (deal-status.ts)
const STATUS_STYLES: Record<TransactionStatus, StatusStyle> = {
  pending: {
    colorVar: 'var(--color-state-warning)',
    bg: 'var(--am-soft-warning-bg)',
    i18nKey: 'wallet.status.pending',
  },
  confirmed: {
    colorVar: 'var(--color-state-success)',
    bg: 'var(--am-soft-success-bg)',
    i18nKey: 'wallet.status.confirmed',
  },
  failed: {
    colorVar: 'var(--color-state-destructive)',
    bg: 'var(--am-soft-destructive-bg)',
    i18nKey: 'wallet.status.failed',
  },
};

type TransactionStatusBadgeProps = {
  status: TransactionStatus;
};

export function TransactionStatusBadge({ status }: TransactionStatusBadgeProps) {
  const { t } = useTranslation();
  const style = STATUS_STYLES[status];

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: 6,
        backgroundColor: style.bg,
        whiteSpace: 'nowrap',
      }}
    >
      <span style={{ color: style.colorVar, fontSize: 12 }}>
        <Text type="caption1" weight="bold">
          {t(style.i18nKey)}
        </Text>
      </span>
    </span>
  );
}
