import { useQuery } from '@tanstack/react-query';
import { creativeLibraryKeys } from '@/shared/api/query-keys';
import { fetchCreative, fetchCreativeVersions } from '../api/creatives-api';

export function useCreativeDetail(id: string | undefined) {
  return useQuery({
    queryKey: creativeLibraryKeys.detail(id ?? ''),
    queryFn: () => fetchCreative(id as string),
    enabled: !!id,
    networkMode: 'online',
  });
}

export function useCreativeVersions(id: string | undefined) {
  return useQuery({
    queryKey: creativeLibraryKeys.versions(id ?? ''),
    queryFn: () => fetchCreativeVersions(id as string),
    enabled: !!id,
    networkMode: 'online',
  });
}
