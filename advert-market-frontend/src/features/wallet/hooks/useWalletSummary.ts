import { useQuery } from '@tanstack/react-query';
import { walletKeys } from '@/shared/api/query-keys';
import { fetchWalletSummary } from '../api/wallet-api';

export function useWalletSummary() {
  return useQuery({
    queryKey: walletKeys.summary,
    queryFn: fetchWalletSummary,
    staleTime: 0,
    networkMode: 'online',
  });
}
