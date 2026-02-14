import { Button, GroupItem, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { Tappable } from '@/shared/ui';
import type { MediaItem } from '../types/creative';

type MediaItemListProps = {
  media: MediaItem[];
  onChange: (media: MediaItem[]) => void;
};

const MEDIA_ICONS: Record<string, string> = {
  PHOTO: '\uD83D\uDDBC\uFE0F',
  VIDEO: '\uD83C\uDFA5',
  GIF: '\uD83D\uDE00',
  DOCUMENT: '\uD83D\uDCC4',
};

export function MediaItemList({ media, onChange }: MediaItemListProps) {
  const { t } = useTranslation();

  const removeMedia = (index: number) => {
    onChange(media.filter((_, i) => i !== index));
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <Text type="subheadline1" weight="medium">
        {t('creatives.form.media')}
      </Text>
      {media.map((item, index) => (
        <GroupItem
          key={`${item.fileId}-${index}`}
          text={`${MEDIA_ICONS[item.type] || '\uD83D\uDCC1'} ${item.type}`}
          description={item.caption || item.fileId.slice(0, 20)}
          after={
            <Tappable
              onClick={() => removeMedia(index)}
              style={{
                border: 'none',
                background: 'transparent',
                color: 'var(--color-state-destructive)',
                cursor: 'pointer',
                fontSize: 14,
              }}
            >
              {t('common.cancel')}
            </Tappable>
          }
        />
      ))}
      <Button text={t('creatives.form.addMedia')} type="secondary" onClick={() => {}} />
    </div>
  );
}
