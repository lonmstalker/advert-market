import { useMutation, useQueryClient } from '@tanstack/react-query';
import { dealKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { negotiateDeal, transitionDeal } from '../api/deals';
import type { NegotiateRequest, TransitionRequest } from '../types/deal';

export function useDealTransition(dealId: string) {
  const queryClient = useQueryClient();
  const haptic = useHaptic();
  const { showSuccess, showError } = useToast();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: dealKeys.detail(dealId) });
    queryClient.invalidateQueries({ queryKey: dealKeys.timeline(dealId) });
    queryClient.invalidateQueries({ queryKey: dealKeys.lists() });
  };

  const transitionMutation = useMutation({
    mutationFn: (request: TransitionRequest) => transitionDeal(dealId, request),
    onSuccess: () => {
      haptic.notificationOccurred('success');
      showSuccess('Done');
      invalidate();
    },
    onError: () => {
      haptic.notificationOccurred('error');
      showError('Action failed');
    },
  });

  const negotiateMutation = useMutation({
    mutationFn: (request: NegotiateRequest) => negotiateDeal(dealId, request),
    onSuccess: () => {
      haptic.notificationOccurred('success');
      showSuccess('Counter-offer sent');
      invalidate();
    },
    onError: () => {
      haptic.notificationOccurred('error');
      showError('Failed to send counter-offer');
    },
  });

  return {
    transition: transitionMutation.mutate,
    negotiate: negotiateMutation.mutate,
    isPending: transitionMutation.isPending || negotiateMutation.isPending,
  };
}
