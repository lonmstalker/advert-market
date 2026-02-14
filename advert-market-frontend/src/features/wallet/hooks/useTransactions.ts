import { useInfiniteQuery } from '@tanstack/react-query';
import { walletKeys } from '@/shared/api/query-keys';
import { fetchTransactions } from '../api/wallet-api';
import type { TransactionFilters } from '../types/wallet';

export function useTransactions(filters?: TransactionFilters, limit?: number) {
  const params = {
    type: filters?.type,
    from: filters?.from,
    to: filters?.to,
  };

  return useInfiniteQuery({
    queryKey: walletKeys.transactionList(params),
    queryFn: ({ pageParam }) =>
      fetchTransactions({
        cursor: pageParam,
        limit: limit ?? 20,
        type: filters?.type,
        from: filters?.from,
        to: filters?.to,
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? (lastPage.nextCursor ?? undefined) : undefined),
    networkMode: 'online',
  });
}
