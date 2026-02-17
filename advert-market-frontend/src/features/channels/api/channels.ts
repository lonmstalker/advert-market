import { z } from 'zod/v4';
import { api } from '@/shared/api/client';
import { type PaginatedResponse, paginatedResponseSchema } from '@/shared/api/types';
import {
  type CatalogFilters,
  type Category,
  type Channel,
  type ChannelDetail,
  type ChannelRegistrationRequest,
  type ChannelResponse,
  type ChannelTeam,
  type ChannelUpdateRequest,
  type ChannelVerifyResponse,
  type CreateDealRequest,
  type CreateDealResponse,
  categorySchema,
  channelDetailSchema,
  channelResponseSchema,
  channelSchema,
  channelTeamSchema,
  channelVerifyResponseSchema,
  createDealResponseSchema,
  type PostType,
  type PricingRule,
  type PricingRuleCreateRequest,
  postTypeSchema,
  pricingRuleSchema,
} from '../types/channel';

export function fetchCategories(): Promise<Category[]> {
  return api.get('/categories', {
    schema: z.array(categorySchema),
  });
}

export function fetchPostTypes(): Promise<PostType[]> {
  return api.get('/post-types', {
    schema: z.array(postTypeSchema),
  });
}

export function fetchChannels(
  filters: CatalogFilters & { cursor?: string; limit?: number },
): Promise<PaginatedResponse<Channel>> {
  const { q, category, categories, languages, minSubs, maxSubs, minPrice, maxPrice, sort, cursor, limit } = filters;
  const categoryParam = category ?? categories?.[0];
  return api.get('/channels', {
    schema: paginatedResponseSchema(channelSchema),
    params: {
      q,
      category: categoryParam,
      // Backend currently supports single category; keep query cache keys stable while
      // allowing chip row multi-select state (`categories`) in UI.
      ...(languages?.length ? { languages: languages.join(',') } : {}),
      minSubs,
      maxSubs,
      minPrice,
      maxPrice,
      sort,
      ...(cursor ? { cursor } : {}),
      limit: limit ?? 20,
    },
  });
}

export function fetchChannelCount(filters: Omit<CatalogFilters, 'sort'>): Promise<number> {
  const { q, category, categories, languages, minSubs, maxSubs, minPrice, maxPrice } = filters;
  const categoryParam = category ?? categories?.[0];
  return api.get('/channels/count', {
    schema: z.number(),
    params: {
      q,
      category: categoryParam,
      ...(languages?.length ? { languages: languages.join(',') } : {}),
      minSubs,
      maxSubs,
      minPrice,
      maxPrice,
    },
  });
}

export function fetchChannelDetail(channelId: number): Promise<ChannelDetail> {
  return api.get(`/channels/${channelId}`, {
    schema: channelDetailSchema,
  });
}

export function fetchChannelTeam(channelId: number): Promise<ChannelTeam> {
  return api.get(`/channels/${channelId}/team`, {
    schema: channelTeamSchema,
  });
}

export function createDeal(request: CreateDealRequest): Promise<CreateDealResponse> {
  return api.post('/deals', request, {
    schema: createDealResponseSchema,
  });
}

export function verifyChannel(channelReference: string): Promise<ChannelVerifyResponse> {
  return api.post(
    '/channels/verify',
    { channelUsername: channelReference },
    {
      schema: channelVerifyResponseSchema,
    },
  );
}

export function registerChannel(request: ChannelRegistrationRequest): Promise<ChannelResponse> {
  return api.post('/channels', request, {
    schema: channelResponseSchema,
  });
}

export function updateChannel(channelId: number, request: ChannelUpdateRequest): Promise<ChannelResponse> {
  return api.put(`/channels/${channelId}`, request, {
    schema: channelResponseSchema,
  });
}

export function createChannelPricingRule(channelId: number, request: PricingRuleCreateRequest): Promise<PricingRule> {
  return api.post(`/channels/${channelId}/pricing`, request, {
    schema: pricingRuleSchema,
  });
}

export function updateChannelPricingRule(
  channelId: number,
  ruleId: number,
  request: PricingRuleCreateRequest,
): Promise<PricingRule> {
  return api.put(`/channels/${channelId}/pricing/${ruleId}`, request, {
    schema: pricingRuleSchema,
  });
}

export function deleteChannelPricingRule(channelId: number, ruleId: number): Promise<void> {
  return api.delete(`/channels/${channelId}/pricing/${ruleId}`);
}

export function fetchMyChannels(): Promise<ChannelResponse[]> {
  return api.get('/channels/my', {
    schema: z.array(channelResponseSchema),
  });
}
