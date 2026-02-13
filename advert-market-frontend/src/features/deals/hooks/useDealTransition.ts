import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { dealKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { negotiateDeal, transitionDeal } from '../api/deals';
import type { NegotiateRequest, TransitionRequest } from '../types/deal';

export function useDealTransition(dealId: string) {
  const { t } = useTranslation();
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
      showSuccess(t('deals.transition.success'));
      invalidate();
    },
    onError: () => {
      haptic.notificationOccurred('error');
      showError(t('deals.transition.error'));
    },
  });

  const negotiateMutation = useMutation({
    mutationFn: (request: NegotiateRequest) => negotiateDeal(dealId, request),
    onSuccess: () => {
      haptic.notificationOccurred('success');
      showSuccess(t('deals.transition.negotiateSuccess'));
      invalidate();
    },
    onError: () => {
      haptic.notificationOccurred('error');
      showError(t('deals.transition.negotiateError'));
    },
  });

  return {
    transition: transitionMutation.mutate,
    negotiate: negotiateMutation.mutate,
    isPending: transitionMutation.isPending || negotiateMutation.isPending,
  };
}
