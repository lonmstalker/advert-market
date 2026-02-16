import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { dealKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { negotiateDeal, transitionDeal } from '../api/deals';
import { EXPECTED_NEXT_STATUS } from '../lib/deal-status';
import type { Deal, DealStatus, NegotiateRequest, TransitionRequest } from '../types/deal';

const FINANCIAL_STATUSES = new Set<DealStatus>([
  'AWAITING_PAYMENT',
  'FUNDED',
  'COMPLETED_RELEASED',
  'REFUNDED',
  'PARTIALLY_REFUNDED',
]);

function isFinancialTransition(currentStatus: DealStatus | undefined): boolean {
  return currentStatus != null && FINANCIAL_STATUSES.has(currentStatus);
}

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
    async onMutate() {
      const detailKey = dealKeys.detail(dealId);
      const previous = queryClient.getQueryData<Deal>(detailKey);

      if (previous && !isFinancialTransition(previous.status)) {
        const nextStatus = EXPECTED_NEXT_STATUS[previous.status];
        if (nextStatus) {
          await queryClient.cancelQueries({ queryKey: detailKey });
          queryClient.setQueryData<Deal>(detailKey, { ...previous, status: nextStatus });
          haptic.notificationOccurred('success');
          return { previous };
        }
      }

      return { previous: undefined };
    },
    onSuccess(_data, _variables, context) {
      if (!context?.previous) {
        haptic.notificationOccurred('success');
      }
      showSuccess(t('deals.transition.success'));
    },
    onError(_error, _variables, context) {
      if (context?.previous) {
        queryClient.setQueryData(dealKeys.detail(dealId), context.previous);
      }
      haptic.notificationOccurred('error');
      showError(t('deals.transition.error'));
    },
    onSettled() {
      invalidate();
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
