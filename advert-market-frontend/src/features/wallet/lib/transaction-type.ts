import type { ComponentType, SVGProps } from 'react';
import { ClipboardIcon, CoinIcon, LockIcon, RefundIcon } from '@/shared/ui/icons';
import type { TransactionDirection, TransactionType } from '../types/wallet';

type TransactionTypeConfig = {
  Icon: ComponentType<SVGProps<SVGSVGElement>>;
  i18nKey: string;
};

const TYPE_CONFIG: Record<TransactionType, TransactionTypeConfig> = {
  escrow_deposit: {
    Icon: LockIcon,
    i18nKey: 'wallet.txType.escrowDeposit',
  },
  payout: {
    Icon: CoinIcon,
    i18nKey: 'wallet.txType.payout',
  },
  refund: {
    Icon: RefundIcon,
    i18nKey: 'wallet.txType.refund',
  },
  commission: {
    Icon: ClipboardIcon,
    i18nKey: 'wallet.txType.commission',
  },
};

const DIRECTION_COLORS: Record<TransactionDirection, string> = {
  income: 'var(--color-state-success)',
  expense: 'var(--color-state-destructive)',
};

export function getTransactionTypeConfig(type: TransactionType): TransactionTypeConfig {
  return TYPE_CONFIG[type];
}

const TYPE_TINT_COLORS: Record<TransactionType, string> = {
  escrow_deposit: 'var(--am-soft-accent-bg)',
  payout: 'var(--am-soft-success-bg)',
  refund: 'var(--am-soft-warning-bg)',
  commission: 'var(--am-soft-secondary-bg)',
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
