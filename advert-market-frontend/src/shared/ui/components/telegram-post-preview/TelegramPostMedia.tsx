import { Image } from '@telegram-tools/ui-kit';
import type { ComponentType, SVGProps } from 'react';
import type { MediaItem } from '@/shared/types/text-entity';
import { FileIcon, ImageIcon, VideoIcon } from '../../icons';

type TelegramPostMediaProps = {
  media: MediaItem[];
};

const MEDIA_ICONS: Record<string, ComponentType<SVGProps<SVGSVGElement>>> = {
  PHOTO: ImageIcon,
  VIDEO: VideoIcon,
  GIF: ImageIcon,
  DOCUMENT: FileIcon,
};

export function TelegramPostMedia({ media }: TelegramPostMediaProps) {
  if (media.length === 0) return null;

  const documents = media.filter((item) => item.type === 'DOCUMENT');
  if (documents.length === media.length) {
    return (
      <div className="am-tg-documents">
        {documents.map((item, index) => {
          const fileRef = item.fileId ?? item.id;
          return (
            <div key={fileRef} className="am-tg-document">
              <div className="am-tg-document-icon">
                <FileIcon className="w-5 h-5" />
              </div>
              <div className="am-tg-document-info">
                <span className="am-tg-document-name">{item.caption || `Document ${index + 1}`}</span>
                <span className="am-tg-document-size">{fileRef.slice(0, 12)}...</span>
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  const visualMedia = media.filter((item) => item.type !== 'DOCUMENT');
  const visibleMedia = visualMedia.slice(0, 4);
  const overflowCount = Math.max(0, visualMedia.length - visibleMedia.length);

  const gridClass =
    visibleMedia.length === 1
      ? 'am-tg-media-grid am-tg-media--single'
      : visibleMedia.length === 2
        ? 'am-tg-media-grid am-tg-media--double'
        : visibleMedia.length === 3
          ? 'am-tg-media-grid am-tg-media--triple'
          : visualMedia.length > 4
            ? 'am-tg-media-grid am-tg-media--mosaic'
            : 'am-tg-media-grid am-tg-media--quad';

  return (
    <div className={gridClass}>
      {visibleMedia.map((item, index) => {
        const Icon = MEDIA_ICONS[item.type] || FileIcon;
        const showOverflow = overflowCount > 0 && index === visibleMedia.length - 1;

        return (
          <div key={`${item.fileId}-${index}`} className="am-tg-media-cell">
            {item.url && (item.type === 'PHOTO' || item.type === 'GIF') ? (
              <Image
                src={item.url}
                alt={item.caption || 'Media'}
                className="am-tg-media-image"
                width="100%"
                height="100%"
                objectFit="cover"
              />
            ) : (
              <div className="am-tg-media-video-overlay">
                {item.type === 'VIDEO' ? (
                  <div className="am-tg-media-video-play" />
                ) : (
                  <Icon className="w-6 h-6 text-white/80" />
                )}
              </div>
            )}
            {showOverflow && <div className="am-tg-media-overflow">+{overflowCount}</div>}
          </div>
        );
      })}
    </div>
  );
}
