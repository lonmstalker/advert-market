import { z } from 'zod/v4';
import { api } from '@/shared/api/client';
import { type PaginatedResponse, paginatedResponseSchema } from '@/shared/api/types';
import {
  type Deal,
  type DealListItem,
  type DealRole,
  type DealTimeline,
  type NegotiateRequest,
  type TransitionRequest,
  dealListItemSchema,
  dealSchema,
  dealTimelineSchema,
} from '../types/deal';

export function fetchDeals(params: {
  role?: DealRole;
  cursor?: string;
  limit?: number;
}): Promise<PaginatedResponse<DealListItem>> {
  return api.get('/deals', {
    schema: paginatedResponseSchema(dealListItemSchema),
    params: {
      role: params.role,
      cursor: params.cursor,
      limit: params.limit ?? 20,
    },
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
