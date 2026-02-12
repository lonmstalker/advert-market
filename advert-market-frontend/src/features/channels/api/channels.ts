import { z } from 'zod/v4';
import { api } from '@/shared/api/client';
import { type PaginatedResponse, paginatedResponseSchema } from '@/shared/api/types';
import {
  type CatalogFilters,
  type Channel,
  type ChannelDetail,
  type ChannelTeam,
  type ChannelTopic,
  type CreateDealRequest,
  type CreateDealResponse,
  channelDetailSchema,
  channelSchema,
  channelTeamSchema,
  channelTopicSchema,
  createDealResponseSchema,
} from '../types/channel';

export function fetchChannelTopics(): Promise<ChannelTopic[]> {
  return api.get('/channels/topics', {
    schema: z.array(channelTopicSchema),
  });
}

export function fetchChannels(
  filters: CatalogFilters & { cursor?: string; limit?: number },
): Promise<PaginatedResponse<Channel>> {
  const { q, topic, minSubs, maxSubs, minPrice, maxPrice, sort, cursor, limit } = filters;
  return api.get('/channels', {
    schema: paginatedResponseSchema(channelSchema),
    params: {
      q,
      topic,
      minSubs,
      maxSubs,
      minPrice,
      maxPrice,
      sort,
      cursor,
      limit: limit ?? 20,
    },
  });
}

export function fetchChannelCount(filters: Omit<CatalogFilters, 'sort'>): Promise<number> {
  const { q, topic, minSubs, maxSubs, minPrice, maxPrice } = filters;
  return api.get('/channels/count', {
    schema: z.number(),
    params: { q, topic, minSubs, maxSubs, minPrice, maxPrice },
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
