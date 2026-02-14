import { useQuery } from '@tanstack/react-query';
import { walletKeys } from '@/shared/api/query-keys';
import { fetchTransactionDetail } from '../api/wallet-api';

export function useTransactionDetail(txId: string) {
  return useQuery({
    queryKey: walletKeys.transactionDetail(txId),
    queryFn: () => fetchTransactionDetail(txId),
    enabled: !!txId,
  });
}
