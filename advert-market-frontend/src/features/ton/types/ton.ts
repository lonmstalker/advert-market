import { z } from 'zod/v4';

export const DepositInfoSchema = z.object({
  escrowAddress: z.string(),
  amountNano: z.string(),
  dealId: z.string(),
  status: z.enum([
    'AWAITING_PAYMENT',
    'TX_DETECTED',
    'CONFIRMING',
    'AWAITING_OPERATOR_REVIEW',
    'CONFIRMED',
    'EXPIRED',
    'UNDERPAID',
    'OVERPAID',
    'REJECTED',
  ]),
  currentConfirmations: z.number().nullable(),
  requiredConfirmations: z.number().nullable(),
  receivedAmountNano: z.string().nullable(),
  txHash: z.string().nullable(),
  expiresAt: z.string().nullable(),
});

export type DepositInfo = z.infer<typeof DepositInfoSchema>;
