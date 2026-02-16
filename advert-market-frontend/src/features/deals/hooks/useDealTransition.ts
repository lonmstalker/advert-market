import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { dealKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { transitionDeal } from '../api/deals';
import type { DealDetailDto, DealStatus, TransitionRequest } from '../types/deal';

const NON_OPTIMISTIC_TARGET_STATUSES = new Set<DealStatus>([
  'AWAITING_PAYMENT',
  'FUNDED',
  'PUBLISHED',
  'DELIVERY_VERIFYING',
  'COMPLETED_RELEASED',
  'REFUNDED',
  'PARTIALLY_REFUNDED',
  'EXPIRED',
]);

function canApplyOptimisticUpdate(targetStatus: DealStatus): boolean {
  return !NON_OPTIMISTIC_TARGET_STATUSES.has(targetStatus);
}

export function useDealTransition(dealId: string) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const haptic = useHaptic();
  const { showSuccess, showError } = useToast();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: dealKeys.detail(dealId) });
    queryClient.invalidateQueries({ queryKey: dealKeys.lists() });
  };

  const transitionMutation = useMutation({
    mutationFn: (request: TransitionRequest) => transitionDeal(dealId, request),
    async onMutate(request) {
      const detailKey = dealKeys.detail(dealId);
      const previous = queryClient.getQueryData<DealDetailDto>(detailKey);

      if (previous && canApplyOptimisticUpdate(request.targetStatus)) {
        await queryClient.cancelQueries({ queryKey: detailKey });
        queryClient.setQueryData<DealDetailDto>(detailKey, {
          ...previous,
          status: request.targetStatus,
        });
        haptic.notificationOccurred('success');
        return { previous };
      }

      return { previous: undefined };
    },
    onSuccess(data, _variables, context) {
      if (!context?.previous) {
        haptic.notificationOccurred('success');
      }

      if (data.status === 'ALREADY_IN_TARGET_STATE') {
        showSuccess(t('deals.transition.already'));
        return;
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

  return {
    transition: transitionMutation.mutate,
    isPending: transitionMutation.isPending,
  };
}
