import { ApiError } from '@/shared/api';
import { api } from '@/shared/api/client';
import { type PaginatedResponse, paginatedResponseSchema } from '@/shared/api/types';
import { z } from 'zod/v4';
import {
  type Transaction,
  type TransactionDetail,
  type TransactionFilters,
  transactionDetailSchema,
  transactionSchema,
  type WalletSummary,
  walletSummarySchema,
} from '../types/wallet';

const WALLET_API_UNAVAILABLE_STATUSES = new Set([404, 405, 501]);
const isMockApiEnabled = import.meta.env.VITE_MOCK_API === 'true';
const frontendTransactionsPageSchema = paginatedResponseSchema(transactionSchema);

function isWalletApiUnavailable(error: unknown): error is ApiError {
  return error instanceof ApiError && WALLET_API_UNAVAILABLE_STATUSES.has(error.status);
}

const EMPTY_SUMMARY: WalletSummary = {
  earnedTotalNano: '0',
  inEscrowNano: '0',
  spentTotalNano: '0',
  activeEscrowNano: '0',
  activeDealsCount: 0,
  completedDealsCount: 0,
};

const EMPTY_TRANSACTIONS_PAGE: PaginatedResponse<Transaction> = {
  items: [],
  nextCursor: null,
  hasNext: false,
};

const backendWalletSummarySchema = z.object({
  pendingBalanceNano: z.coerce.number(),
  availableBalanceNano: z.coerce.number(),
  totalEarnedNano: z.coerce.number(),
});

const backendLedgerEntrySchema = z.object({
  id: z.union([z.number(), z.string()]).optional(),
  dealId: z.string().nullable().optional(),
  entryType: z.string().optional(),
  debitNano: z.coerce.number().optional(),
  creditNano: z.coerce.number().optional(),
  description: z.string().nullable().optional(),
  createdAt: z.string().optional(),
  txRef: z.string().optional(),
  txHash: z.string().nullable().optional(),
  fromAddress: z.string().nullable().optional(),
  toAddress: z.string().nullable().optional(),
});

const backendTransactionsPageSchema = z.object({
  items: z.array(backendLedgerEntrySchema).default([]),
  nextCursor: z.string().nullable().optional(),
  hasNext: z.boolean().optional(),
});

function notFoundTransactionError(txId: string): ApiError {
  return new ApiError(404, {
    type: 'about:blank',
    title: 'Not Found',
    status: 404,
    detail: `Transaction ${txId} not found`,
  });
}

function normalizeWalletSummary(payload: unknown): WalletSummary {
  const frontend = walletSummarySchema.safeParse(payload);
  if (frontend.success) {
    return frontend.data;
  }

  const backend = backendWalletSummarySchema.safeParse(payload);
  if (!backend.success) {
    throw backend.error;
  }
  const isOwnerSummary = backend.data.totalEarnedNano > 0;
  if (isOwnerSummary) {
    return {
      earnedTotalNano: String(backend.data.totalEarnedNano),
      inEscrowNano: String(backend.data.pendingBalanceNano),
      spentTotalNano: '0',
      activeEscrowNano: '0',
      activeDealsCount: 0,
      completedDealsCount: 0,
    };
  }
  return {
    earnedTotalNano: '0',
    inEscrowNano: '0',
    spentTotalNano: String(backend.data.availableBalanceNano),
    activeEscrowNano: String(backend.data.pendingBalanceNano),
    activeDealsCount: 0,
    completedDealsCount: 0,
  };
}

function mapEntryTypeToTransactionType(
  entryType: string | undefined,
  direction: 'income' | 'expense',
): Transaction['type'] {
  switch (entryType) {
    case 'ESCROW_DEPOSIT':
    case 'PARTIAL_DEPOSIT':
    case 'PARTIAL_DEPOSIT_PROMOTE':
      return 'escrow_deposit';
    case 'ESCROW_REFUND':
    case 'PARTIAL_REFUND':
    case 'OVERPAYMENT_REFUND':
    case 'LATE_DEPOSIT_REFUND':
      return 'refund';
    case 'PLATFORM_COMMISSION':
    case 'COMMISSION_SWEEP':
    case 'NETWORK_FEE':
    case 'NETWORK_FEE_REFUND':
    case 'FEE_ADJUSTMENT':
    case 'DUST_WRITEOFF':
      return 'commission';
    case 'OWNER_PAYOUT':
    case 'ESCROW_RELEASE':
    case 'OWNER_WITHDRAWAL':
      return 'payout';
    case 'REVERSAL':
      return direction === 'income' ? 'refund' : 'commission';
    default:
      return direction === 'income' ? 'payout' : 'commission';
  }
}

function toFrontendTransaction(entry: z.infer<typeof backendLedgerEntrySchema>): Transaction {
  const creditNano = entry.creditNano ?? 0;
  const debitNano = entry.debitNano ?? 0;
  const direction: Transaction['direction'] = creditNano > 0 ? 'income' : 'expense';
  const amountNano = String(creditNano > 0 ? creditNano : Math.max(debitNano, 0));
  const id = String(entry.id ?? entry.txRef ?? `${entry.entryType ?? 'tx'}-${entry.createdAt ?? '0'}`);

  return transactionSchema.parse({
    id,
    type: mapEntryTypeToTransactionType(entry.entryType, direction),
    status: 'confirmed',
    amountNano,
    direction,
    dealId: entry.dealId ?? null,
    channelTitle: null,
    description: entry.description?.trim() || entry.entryType || 'Transaction',
    createdAt: entry.createdAt ?? new Date(0).toISOString(),
  });
}

function normalizeTransactionsPage(payload: unknown): PaginatedResponse<Transaction> {
  const frontend = frontendTransactionsPageSchema.safeParse(payload);
  if (frontend.success) {
    return frontend.data;
  }

  const backend = backendTransactionsPageSchema.safeParse(payload);
  if (!backend.success) {
    throw backend.error;
  }

  const nextCursor = backend.data.nextCursor ?? null;
  const hasNext = backend.data.hasNext ?? nextCursor !== null;
  return {
    items: backend.data.items.map(toFrontendTransaction),
    nextCursor,
    hasNext,
  };
}

function normalizeTransactionDetail(payload: unknown): TransactionDetail {
  const frontend = transactionDetailSchema.safeParse(payload);
  if (frontend.success) {
    return frontend.data;
  }

  const backend = backendLedgerEntrySchema.safeParse(payload);
  if (!backend.success) {
    throw backend.error;
  }
  const transaction = toFrontendTransaction(backend.data);
  return {
    ...transaction,
    txHash: backend.data.txHash ?? null,
    fromAddress: backend.data.fromAddress ?? null,
    toAddress: backend.data.toAddress ?? null,
    commissionNano: null,
  };
}

async function loadTransactionDetailFromList(txId: string): Promise<TransactionDetail> {
  const page = await fetchTransactions({ limit: 100 });
  const transaction = page.items.find((item) => item.id === txId);
  if (!transaction) {
    throw notFoundTransactionError(txId);
  }
  return {
    ...transaction,
    txHash: null,
    fromAddress: null,
    toAddress: null,
    commissionNano: null,
  };
}

export async function fetchWalletSummary(): Promise<WalletSummary> {
  try {
    const payload = await api.get<unknown>('/wallet/summary');
    return normalizeWalletSummary(payload);
  } catch (error: unknown) {
    if (!isMockApiEnabled && isWalletApiUnavailable(error)) {
      return EMPTY_SUMMARY;
    }
    throw error;
  }
}

export function fetchTransactions(params: {
  cursor?: string;
  limit?: number;
  type?: TransactionFilters['type'];
  from?: string;
  to?: string;
}): Promise<PaginatedResponse<Transaction>> {
  return api
    .get<unknown>('/wallet/transactions', {
      params: {
        cursor: params.cursor,
        limit: params.limit ?? 20,
        type: params.type,
        from: params.from,
        to: params.to,
      },
    })
    .then((payload) => normalizeTransactionsPage(payload))
    .catch((error: unknown) => {
      if (!isMockApiEnabled && isWalletApiUnavailable(error)) {
        return EMPTY_TRANSACTIONS_PAGE;
      }
      throw error;
    });
}

export async function fetchTransactionDetail(txId: string): Promise<TransactionDetail> {
  try {
    const payload = await api.get<unknown>(`/wallet/transactions/${txId}`);
    return normalizeTransactionDetail(payload);
  } catch (error: unknown) {
    if (!isMockApiEnabled && isWalletApiUnavailable(error)) {
      return loadTransactionDetailFromList(txId);
    }
    throw error;
  }
}
