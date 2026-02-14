import { useInfiniteQuery } from '@tanstack/react-query';
import { creativeLibraryKeys } from '@/shared/api/query-keys';
import { fetchCreatives } from '../api/creatives-api';

export function useCreatives(limit?: number) {
  return useInfiniteQuery({
    queryKey: creativeLibraryKeys.lists(),
    queryFn: ({ pageParam }) =>
      fetchCreatives({
        cursor: pageParam,
        limit: limit ?? 20,
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? (lastPage.nextCursor ?? undefined) : undefined),
    networkMode: 'online',
  });
}
