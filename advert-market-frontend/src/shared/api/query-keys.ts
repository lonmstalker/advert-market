import type { PaginationParams } from './types';

export const authKeys = {
  session: ['auth', 'session'] as const,
};

export const dealKeys = {
  all: ['deals'] as const,
  lists: () => [...dealKeys.all, 'list'] as const,
  list: (params?: PaginationParams) => [...dealKeys.lists(), params] as const,
  details: () => [...dealKeys.all, 'detail'] as const,
  detail: (id: string) => [...dealKeys.details(), id] as const,
  timeline: (id: string) => [...dealKeys.detail(id), 'timeline'] as const,
  deposit: (id: string) => [...dealKeys.detail(id), 'deposit'] as const,
  escrow: (id: string) => [...dealKeys.detail(id), 'escrow'] as const,
};

export const channelKeys = {
  all: ['channels'] as const,
  lists: () => [...channelKeys.all, 'list'] as const,
  list: (params?: PaginationParams & Record<string, string | number | undefined>) =>
    [...channelKeys.lists(), params] as const,
  details: () => [...channelKeys.all, 'detail'] as const,
  detail: (id: number) => [...channelKeys.details(), id] as const,
  team: (id: number) => [...channelKeys.detail(id), 'team'] as const,
  topics: () => [...channelKeys.all, 'topics'] as const,
  count: (params?: Record<string, string | number | undefined>) => [...channelKeys.all, 'count', params] as const,
};

export const creativeKeys = {
  current: (dealId: string) => ['creative', dealId, 'current'] as const,
  history: (dealId: string) => ['creative', dealId, 'history'] as const,
  brief: (dealId: string) => ['creative', dealId, 'brief'] as const,
};

export const disputeKeys = {
  detail: (dealId: string) => ['dispute', dealId] as const,
};
