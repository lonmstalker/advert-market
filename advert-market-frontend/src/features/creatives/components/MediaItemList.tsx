import { Button, GroupItem, Text } from '@telegram-tools/ui-kit';
import type { ComponentType, SVGProps } from 'react';
import { useTranslation } from 'react-i18next';
import { Tappable } from '@/shared/ui';
import { FileIcon, ImageIcon, VideoIcon } from '@/shared/ui/icons';
import type { MediaItem } from '../types/creative';

type MediaItemListProps = {
  media: MediaItem[];
  onChange: (media: MediaItem[]) => void;
};

const MEDIA_ICONS: Record<string, ComponentType<SVGProps<SVGSVGElement>>> = {
  PHOTO: ImageIcon,
  VIDEO: VideoIcon,
  GIF: ImageIcon,
  DOCUMENT: FileIcon,
};

const mediaIconStyle = { width: 20, height: 20, color: 'var(--color-foreground-secondary)' };

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
      {media.map((item, index) => {
        const Icon = MEDIA_ICONS[item.type] || FileIcon;
        return (
          <GroupItem
            key={`${item.fileId}-${index}`}
            before={<Icon style={mediaIconStyle} />}
            text={item.type}
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
        );
      })}
      <Button text={t('creatives.form.addMedia')} type="secondary" disabled onClick={() => {}} />
    </div>
  );
}
