import { ApiError } from '@/shared/api';
import { api } from '@/shared/api/client';
import { type PaginatedResponse, paginatedResponseSchema } from '@/shared/api/types';
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

export function fetchWalletSummary(): Promise<WalletSummary> {
  return api.get('/wallet/summary', { schema: walletSummarySchema }).catch((error: unknown) => {
    if (!isMockApiEnabled && isWalletApiUnavailable(error)) {
      return EMPTY_SUMMARY;
    }
    throw error;
  });
}

export function fetchTransactions(params: {
  cursor?: string;
  limit?: number;
  type?: TransactionFilters['type'];
  from?: string;
  to?: string;
}): Promise<PaginatedResponse<Transaction>> {
  return api
    .get('/wallet/transactions', {
      schema: paginatedResponseSchema(transactionSchema),
      params: {
        cursor: params.cursor,
        limit: params.limit ?? 20,
        type: params.type,
        from: params.from,
        to: params.to,
      },
    })
    .catch((error: unknown) => {
      if (!isMockApiEnabled && isWalletApiUnavailable(error)) {
        return { items: [], nextCursor: null, hasNext: false };
      }
      throw error;
    });
}

export function fetchTransactionDetail(txId: string): Promise<TransactionDetail> {
  return api.get(`/wallet/transactions/${txId}`, {
    schema: transactionDetailSchema,
  });
}
