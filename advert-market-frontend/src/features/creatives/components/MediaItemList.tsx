import { GroupItem, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import type { ComponentType, CSSProperties, SVGProps } from 'react';
import { useTranslation } from 'react-i18next';
import { Tappable } from '@/shared/ui';
import { pulse } from '@/shared/ui/animations';
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

const dropZoneStyle: CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  padding: '28px 16px',
  borderRadius: 12,
  border: '2px dashed color-mix(in srgb, var(--color-accent-primary) 30%, transparent)',
  background: 'color-mix(in srgb, var(--color-accent-primary) 5%, transparent)',
  cursor: 'default',
  position: 'relative',
  overflow: 'hidden',
};

const comingSoonBadge: CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  padding: '2px 10px',
  borderRadius: 20,
  background: 'color-mix(in srgb, var(--color-accent-primary) 12%, transparent)',
  fontSize: 11,
  fontWeight: 600,
  color: 'var(--color-accent-primary)',
  letterSpacing: 0.5,
  textTransform: 'uppercase',
};

export function MediaItemList({ media, onChange }: MediaItemListProps) {
  const { t } = useTranslation();

  const removeMedia = (index: number) => {
    onChange(media.filter((_, i) => i !== index));
  };

  return (
    <div className="flex flex-col gap-2">
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
      <div style={dropZoneStyle}>
        <motion.div {...pulse}>
          <ImageIcon style={{ width: 32, height: 32, color: 'var(--color-accent-primary)', opacity: 0.5 }} />
        </motion.div>
        <Text type="caption1" color="secondary">
          {t('creatives.form.addMedia')}
        </Text>
        <span style={comingSoonBadge}>Coming soon</span>
      </div>
    </div>
  );
}
