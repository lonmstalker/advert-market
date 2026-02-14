import type { ComponentType, SVGProps } from 'react';
import type { MediaItem } from '@/shared/types/text-entity';
import { FileIcon, ImageIcon, VideoIcon } from '../../icons';
import { mediaContainer, mediaImage, mediaPlaceholder } from './styles';

type TelegramPostMediaProps = {
  media: MediaItem[];
};

const MEDIA_ICONS: Record<string, ComponentType<SVGProps<SVGSVGElement>>> = {
  PHOTO: ImageIcon,
  VIDEO: VideoIcon,
  GIF: ImageIcon,
  DOCUMENT: FileIcon,
};

const placeholderIconStyle = { width: 20, height: 20 };

export function TelegramPostMedia({ media }: TelegramPostMediaProps) {
  if (media.length === 0) return null;

  return (
    <div style={mediaContainer}>
      {media.map((item, index) => {
        const Icon = MEDIA_ICONS[item.type] || FileIcon;
        return (
          <div key={`${item.fileId}-${index}`}>
            {item.url ? (
              item.type === 'PHOTO' || item.type === 'GIF' ? (
                <img src={item.url} alt={item.caption || 'Media'} style={mediaImage} />
              ) : (
                <div style={mediaPlaceholder}>
                  <Icon style={placeholderIconStyle} />
                  {item.caption || item.type}
                </div>
              )
            ) : (
              <div style={mediaPlaceholder}>
                <Icon style={placeholderIconStyle} />
                {item.type} ({item.fileId.slice(0, 12)}...)
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
