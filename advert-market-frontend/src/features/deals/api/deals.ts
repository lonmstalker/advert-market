import { ApiError } from '@/shared/api';
import { api } from '@/shared/api/client';
import { type PaginatedResponse, paginatedResponseSchema } from '@/shared/api/types';
import {
  type Deal,
  type DealListItem,
  type DealRole,
  type DealTimeline,
  dealListItemSchema,
  dealSchema,
  dealTimelineSchema,
  type NegotiateRequest,
  type TransitionRequest,
} from '../types/deal';

const DEAL_API_UNAVAILABLE_STATUSES = new Set([404, 405, 501]);
const isMockApiEnabled = import.meta.env.VITE_MOCK_API === 'true';

function isDealApiUnavailable(error: unknown): error is ApiError {
  return error instanceof ApiError && DEAL_API_UNAVAILABLE_STATUSES.has(error.status);
}

export function fetchDeals(params: {
  role?: DealRole;
  cursor?: string;
  limit?: number;
  statuses?: string[];
}): Promise<PaginatedResponse<DealListItem>> {
  return api
    .get('/deals', {
      schema: paginatedResponseSchema(dealListItemSchema),
      params: {
        role: params.role,
        cursor: params.cursor,
        limit: params.limit ?? 20,
        statuses: params.statuses?.join(','),
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

export function fetchDeal(dealId: string): Promise<Deal> {
  return api.get(`/deals/${dealId}`, {
    schema: dealSchema,
  });
}

export function fetchDealTimeline(dealId: string): Promise<DealTimeline> {
  return api.get(`/deals/${dealId}/timeline`, {
    schema: dealTimelineSchema,
  });
}

export function transitionDeal(dealId: string, request: TransitionRequest): Promise<Deal> {
  return api.post(`/deals/${dealId}/transition`, request, {
    schema: dealSchema,
  });
}

export function negotiateDeal(dealId: string, request: NegotiateRequest): Promise<Deal> {
  return api.post(`/deals/${dealId}/negotiate`, request, {
    schema: dealSchema,
  });
}
