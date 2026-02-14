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
  type ChannelVerifyResponse,
  type CreateDealRequest,
  type CreateDealResponse,
  categorySchema,
  channelDetailSchema,
  channelRegistrationRequestSchema,
  channelResponseSchema,
  channelSchema,
  channelTeamSchema,
  channelVerifyResponseSchema,
  createDealResponseSchema,
} from '../types/channel';

export function fetchCategories(): Promise<Category[]> {
  return api.get('/categories', {
    schema: z.array(categorySchema),
  });
}

export function fetchChannels(
  filters: CatalogFilters & { cursor?: string; limit?: number },
): Promise<PaginatedResponse<Channel>> {
  const { q, category, minSubs, maxSubs, minPrice, maxPrice, sort, cursor, limit } = filters;
  return api.get('/channels', {
    schema: paginatedResponseSchema(channelSchema),
    params: {
      q,
      category,
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
  const { q, category, minSubs, maxSubs, minPrice, maxPrice } = filters;
  return api.get('/channels/count', {
    schema: z.number(),
    params: { q, category, minSubs, maxSubs, minPrice, maxPrice },
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

export function verifyChannel(username: string): Promise<ChannelVerifyResponse> {
  return api.post('/channels/verify', { channelUsername: username }, {
    schema: channelVerifyResponseSchema,
  });
}

export function registerChannel(request: ChannelRegistrationRequest): Promise<ChannelResponse> {
  return api.post('/channels', request, {
    schema: channelResponseSchema,
  });
}
