import { useQuery } from '@tanstack/react-query';
import { dealKeys } from '@/shared/api/query-keys';
import { fetchDeal, fetchDealTimeline } from '../api/deals';
import { getPollingInterval } from '../lib/deal-status';

export function useDealDetail(dealId: string) {
  const dealQuery = useQuery({
    queryKey: dealKeys.detail(dealId),
    queryFn: () => fetchDeal(dealId),
  });

  const timelineQuery = useQuery({
    queryKey: dealKeys.timeline(dealId),
    queryFn: () => fetchDealTimeline(dealId),
    enabled: !!dealQuery.data,
    refetchInterval: dealQuery.data ? getPollingInterval(dealQuery.data.status) : false,
  });

  return {
    deal: dealQuery.data,
    timeline: timelineQuery.data,
    isLoading: dealQuery.isLoading,
    isError: dealQuery.isError,
    refetch: () => {
      dealQuery.refetch();
      timelineQuery.refetch();
    },
  };
}
