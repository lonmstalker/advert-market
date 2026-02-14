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

export function getAmountColor(_type: TransactionType, direction: TransactionDirection): string {
  return DIRECTION_COLORS[direction];
}

export function formatAmountWithSign(amount: string, direction: TransactionDirection): string {
  return direction === 'income' ? `+${amount}` : `\u2212${amount}`;
}
