import type { MediaItem } from '@/shared/types/text-entity';
import { mediaContainer, mediaImage, mediaPlaceholder } from './styles';

type TelegramPostMediaProps = {
  media: MediaItem[];
};

const MEDIA_ICONS: Record<string, string> = {
  PHOTO: '\uD83D\uDDBC\uFE0F',
  VIDEO: '\uD83C\uDFA5',
  GIF: '\uD83D\uDE00',
  DOCUMENT: '\uD83D\uDCC4',
};

export function TelegramPostMedia({ media }: TelegramPostMediaProps) {
  if (media.length === 0) return null;

  return (
    <div style={mediaContainer}>
      {media.map((item, index) => (
        <div key={`${item.fileId}-${index}`}>
          {item.url ? (
            item.type === 'PHOTO' || item.type === 'GIF' ? (
              <img src={item.url} alt={item.caption || 'Media'} style={mediaImage} />
            ) : (
              <div style={mediaPlaceholder}>
                {MEDIA_ICONS[item.type] || '\uD83D\uDCC1'} {item.caption || item.type}
              </div>
            )
          ) : (
            <div style={mediaPlaceholder}>
              {MEDIA_ICONS[item.type] || '\uD83D\uDCC1'} {item.type} ({item.fileId.slice(0, 12)}...)
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
