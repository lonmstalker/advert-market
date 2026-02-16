import { useQuery } from '@tanstack/react-query';
import { dealKeys } from '@/shared/api/query-keys';
import { fetchDeal } from '../api/deals';
import { getPollingInterval } from '../lib/deal-status';

export function useDealDetail(dealId: string) {
  const dealQuery = useQuery({
    queryKey: dealKeys.detail(dealId),
    queryFn: () => fetchDeal(dealId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (!status) return false;
      return getPollingInterval(status);
    },
  });

  return {
    deal: dealQuery.data,
    timeline: dealQuery.data?.timeline,
    isLoading: dealQuery.isLoading,
    isError: dealQuery.isError,
    refetch: () => dealQuery.refetch(),
  };
}
