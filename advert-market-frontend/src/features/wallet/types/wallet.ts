import { z } from 'zod/v4';

export const TRANSACTION_TYPES = ['escrow_deposit', 'payout', 'refund', 'commission'] as const;
export const TRANSACTION_STATUSES = ['pending', 'confirmed', 'failed'] as const;
export const TRANSACTION_DIRECTIONS = ['income', 'expense'] as const;

export const transactionTypeSchema = z.enum(TRANSACTION_TYPES);
export const transactionStatusSchema = z.enum(TRANSACTION_STATUSES);
export const transactionDirectionSchema = z.enum(TRANSACTION_DIRECTIONS);

export type TransactionType = z.infer<typeof transactionTypeSchema>;
export type TransactionStatus = z.infer<typeof transactionStatusSchema>;
export type TransactionDirection = z.infer<typeof transactionDirectionSchema>;

export const walletSummarySchema = z.object({
  earnedTotalNano: z.string(),
  inEscrowNano: z.string(),
  spentTotalNano: z.string(),
  activeEscrowNano: z.string(),
  activeDealsCount: z.number(),
  completedDealsCount: z.number(),
});

export type WalletSummary = z.infer<typeof walletSummarySchema>;

export const transactionSchema = z.object({
  id: z.string(),
  type: transactionTypeSchema,
  status: transactionStatusSchema,
  amountNano: z.string(),
  direction: transactionDirectionSchema,
  dealId: z.nullable(z.string()),
  channelTitle: z.nullable(z.string()),
  description: z.string(),
  createdAt: z.string(),
});

export type Transaction = z.infer<typeof transactionSchema>;

export const transactionDetailSchema = transactionSchema.extend({
  txHash: z.nullable(z.string()),
  fromAddress: z.nullable(z.string()),
  toAddress: z.nullable(z.string()),
  commissionNano: z.nullable(z.string()),
});

export type TransactionDetail = z.infer<typeof transactionDetailSchema>;

export type TransactionFilters = {
  type?: TransactionType;
  from?: string;
  to?: string;
};
