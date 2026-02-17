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
  categories: () => [...channelKeys.all, 'categories'] as const,
  postTypes: () => [...channelKeys.all, 'post-types'] as const,
  count: (params?: Record<string, string | number | undefined>) => [...channelKeys.all, 'count', params] as const,
  my: () => [...channelKeys.all, 'my'] as const,
};

export const creativeKeys = {
  current: (dealId: string) => ['creative', dealId, 'current'] as const,
  history: (dealId: string) => ['creative', dealId, 'history'] as const,
  brief: (dealId: string) => ['creative', dealId, 'brief'] as const,
};

export const creativeLibraryKeys = {
  all: ['creativeLibrary'] as const,
  lists: () => [...creativeLibraryKeys.all, 'list'] as const,
  list: (params?: Record<string, string | number | undefined>) => [...creativeLibraryKeys.lists(), params] as const,
  details: () => [...creativeLibraryKeys.all, 'detail'] as const,
  detail: (id: string) => [...creativeLibraryKeys.details(), id] as const,
  versions: (id: string) => [...creativeLibraryKeys.detail(id), 'versions'] as const,
};

export const disputeKeys = {
  detail: (dealId: string) => ['dispute', dealId] as const,
};

export const profileKeys = {
  me: ['profile'] as const,
};

export const walletKeys = {
  summary: ['wallet', 'summary'] as const,
  transactions: () => ['wallet', 'transactions'] as const,
  transactionList: (params?: Record<string, string | undefined>) => [...walletKeys.transactions(), params] as const,
  transactionDetail: (txId: string) => [...walletKeys.transactions(), txId] as const,
};
