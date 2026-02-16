import { ApiError } from '@/shared/api';
import { api } from '@/shared/api/client';
import { type PaginatedResponse, paginatedResponseSchema } from '@/shared/api/types';
import {
  type DealDepositInfo,
  type DealDetailDto,
  type DealDto,
  type DealStatus,
  type DealTransitionResponse,
  type TransitionRequest,
  dealDepositInfoSchema,
  dealDetailDtoSchema,
  dealDtoSchema,
  dealTransitionResponseSchema,
} from '../types/deal';

const DEAL_API_UNAVAILABLE_STATUSES = new Set([404, 405, 501]);
const isMockApiEnabled = import.meta.env.VITE_MOCK_API === 'true';

function isDealApiUnavailable(error: unknown): error is ApiError {
  return error instanceof ApiError && DEAL_API_UNAVAILABLE_STATUSES.has(error.status);
}

export function fetchDeals(params: {
  cursor?: string;
  limit?: number;
  status?: DealStatus;
}): Promise<PaginatedResponse<DealDto>> {
  return api
    .get('/deals', {
      schema: paginatedResponseSchema(dealDtoSchema),
      params: {
        cursor: params.cursor,
        limit: params.limit ?? 20,
        status: params.status,
      },
    })
    .catch((error: unknown) => {
      if (!isMockApiEnabled && isDealApiUnavailable(error)) {
        return {
          items: [],
          nextCursor: null,
          hasNext: false,
        };
      }
      throw error;
    });
}

export function fetchDeal(dealId: string): Promise<DealDetailDto> {
  return api.get(`/deals/${dealId}`, {
    schema: dealDetailDtoSchema,
  });
}

export function fetchDealDeposit(dealId: string): Promise<DealDepositInfo> {
  return api.get(`/deals/${dealId}/deposit`, {
    schema: dealDepositInfoSchema,
  });
}

export function transitionDeal(dealId: string, request: TransitionRequest): Promise<DealTransitionResponse> {
  return api.post(`/deals/${dealId}/transition`, request, {
    schema: dealTransitionResponseSchema,
  });
}
