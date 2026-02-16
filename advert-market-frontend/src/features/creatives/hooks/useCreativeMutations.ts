import { useMutation, useQueryClient } from '@tanstack/react-query';
import { creativeLibraryKeys } from '@/shared/api/query-keys';
import {
  type CreateCreativeRequest,
  createCreative,
  deleteCreativeMedia,
  deleteCreative,
  type UpdateCreativeRequest,
  uploadCreativeMedia,
  updateCreative,
} from '../api/creatives-api';

export function useCreateCreative() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (req: CreateCreativeRequest) => createCreative(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: creativeLibraryKeys.all });
    },
  });
}

export function useUpdateCreative(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (req: UpdateCreativeRequest) => updateCreative(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: creativeLibraryKeys.all });
    },
  });
}

export function useDeleteCreative() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => deleteCreative(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: creativeLibraryKeys.all });
    },
  });
}

export function useUploadCreativeMedia() {
  return useMutation({
    mutationFn: ({
      file,
      mediaType,
      caption,
    }: {
      file: File;
      mediaType: 'PHOTO' | 'VIDEO' | 'GIF' | 'DOCUMENT';
      caption?: string;
    }) => uploadCreativeMedia(file, mediaType, caption),
  });
}

export function useDeleteCreativeMedia() {
  return useMutation({
    mutationFn: (mediaId: string) => deleteCreativeMedia(mediaId),
  });
}
