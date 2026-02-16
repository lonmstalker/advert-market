import { GroupItem, Text } from '@telegram-tools/ui-kit';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { Tappable } from '@/shared/ui';
import { FileIcon, ImageIcon, VideoIcon } from '@/shared/ui/icons';
import type { MediaItem, MediaType } from '../types/creative';
import { ensureMediaDefaults, toMediaTypeByMime } from '../types/creative';

type MediaItemListProps = {
  media: MediaItem[];
  onChange: (media: MediaItem[]) => void;
  onUploadMedia?: (file: File, mediaType: MediaType) => Promise<MediaItem>;
  onDeleteMedia?: (mediaId: string) => Promise<void>;
};

const MEDIA_ICON_BY_TYPE = {
  PHOTO: ImageIcon,
  GIF: ImageIcon,
  VIDEO: VideoIcon,
  DOCUMENT: FileIcon,
} as const;

function formatFileSize(sizeBytes: number): string {
  if (sizeBytes < 1024) return `${sizeBytes} B`;
  const kb = sizeBytes / 1024;
  if (kb < 1024) return `${Math.round(kb)} KB`;
  const mb = kb / 1024;
  return `${mb.toFixed(1)} MB`;
}

export function MediaItemList({ media, onChange, onUploadMedia, onDeleteMedia }: MediaItemListProps) {
  const { t } = useTranslation();
  const haptic = useHaptic();
  const inputRef = useRef<HTMLInputElement>(null);
  const [isUploading, setIsUploading] = useState(false);

  const removeMedia = async (index: number) => {
    const target = media[index];
    if (target && onDeleteMedia) {
      try {
        await onDeleteMedia(target.id);
      } catch {
        // Keep the form responsive even if backend deletion fails.
      }
    }
    onChange(media.filter((_, idx) => idx !== index));
  };

  const addMediaFromFile = async (file: File) => {
    const mediaType = toMediaTypeByMime(file.type);
    if (onUploadMedia) {
      setIsUploading(true);
      try {
        const uploaded = await onUploadMedia(file, mediaType);
        onChange([...media, uploaded]);
      } finally {
        setIsUploading(false);
      }
      return;
    }

    const objectUrl = URL.createObjectURL(file);
    onChange([
      ...media,
      ensureMediaDefaults({
        type: mediaType,
        url: objectUrl,
        fileName: file.name,
        fileSize: formatFileSize(file.size),
        mimeType: file.type || 'application/octet-stream',
        sizeBytes: file.size,
      }),
    ]);
  };

  return (
    <div className="am-section">
      <Text type="subheadline2" color="secondary">
        {t('creatives.form.media')}
      </Text>

      {media.map((item, index) => {
        const Icon = MEDIA_ICON_BY_TYPE[item.type] ?? FileIcon;
        return (
          <GroupItem
            key={item.id}
            before={<Icon style={{ width: 20, height: 20, color: 'var(--color-foreground-secondary)' }} />}
            text={item.fileName || item.type}
            description={item.fileSize || formatFileSize(item.sizeBytes)}
            after={
              <Tappable
                onClick={() => {
                  haptic.impactOccurred('light');
                  void removeMedia(index);
                }}
                className="border-0 bg-transparent text-sm text-destructive"
                aria-label={t('creatives.form.removeMedia')}
              >
                {t('common.cancel')}
              </Tappable>
            }
          />
        );
      })}

      <input
        ref={inputRef}
        type="file"
        className="hidden"
        accept="image/*,video/*,application/pdf,.doc,.docx,.zip,.txt"
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) {
            void addMediaFromFile(file);
          }
          event.currentTarget.value = '';
        }}
      />

      <Tappable
        onClick={() => {
          haptic.impactOccurred('light');
          inputRef.current?.click();
        }}
        className="flex items-center justify-center rounded-row border border-separator bg-bg-base px-4 py-3 text-sm text-accent"
        disabled={isUploading}
      >
        {isUploading ? t('common.loading') : `+ ${t('creatives.form.addMedia')}`}
      </Tappable>
    </div>
  );
}
