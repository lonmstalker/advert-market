import type { TransactionDirection, TransactionType } from '../types/wallet';

type TransactionTypeConfig = {
  icon: string;
  i18nKey: string;
};

const TYPE_CONFIG: Record<TransactionType, TransactionTypeConfig> = {
  escrow_deposit: {
    icon: '\uD83D\uDD12',
    i18nKey: 'wallet.txType.escrowDeposit',
  },
  payout: {
    icon: '\uD83D\uDCB0',
    i18nKey: 'wallet.txType.payout',
  },
  refund: {
    icon: '\u21A9\uFE0F',
    i18nKey: 'wallet.txType.refund',
  },
  commission: {
    icon: '\uD83D\uDCCB',
    i18nKey: 'wallet.txType.commission',
  },
};

const DIRECTION_COLORS: Record<TransactionDirection, string> = {
  income: 'var(--color-success)',
  expense: 'var(--color-destructive)',
};

export function getTransactionTypeConfig(type: TransactionType): TransactionTypeConfig {
  return TYPE_CONFIG[type];
}

const TYPE_TINT_COLORS: Record<TransactionType, string> = {
  escrow_deposit: 'rgba(0, 122, 255, 0.1)',
  payout: 'rgba(52, 199, 89, 0.1)',
  refund: 'rgba(255, 159, 10, 0.1)',
  commission: 'rgba(142, 142, 147, 0.1)',
};

export function getTransactionTypeTint(type: TransactionType): string {
  return TYPE_TINT_COLORS[type];
}

export function getAmountColor(_type: TransactionType, direction: TransactionDirection): string {
  return DIRECTION_COLORS[direction];
}

export function formatAmountWithSign(amount: string, direction: TransactionDirection): string {
  return direction === 'income' ? `+${amount}` : `\u2212${amount}`;
}
