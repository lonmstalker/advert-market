import { GroupItem, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { formatRelativeTime } from '@/shared/lib/date-format';
import { pressScale } from '@/shared/ui/animations';
import { ImageIcon, LinkIcon } from '@/shared/ui/icons';
import type { CreativeTemplate } from '../types/creative';

type CreativeListItemProps = {
  creative: CreativeTemplate;
  onClick: () => void;
};

export function CreativeListItem({ creative, onClick }: CreativeListItemProps) {
  const { t, i18n } = useTranslation();
  const mediaCount = creative.draft.media.length;
  const buttonCount = creative.draft.buttons.filter((b) => b.text && b.url).length;
  const textPreview = creative.draft.text.length > 60 ? `${creative.draft.text.slice(0, 60)}...` : creative.draft.text;

  const chips: string[] = [];
  if (mediaCount > 0) chips.push(t('creatives.mediaCount', { count: mediaCount }));
  if (buttonCount > 0) chips.push(t('creatives.buttonsCount', { count: buttonCount }));

  return (
    <motion.div {...pressScale}>
      <GroupItem
        text={creative.title}
        description={textPreview}
        after={
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 2 }}>
            <Text type="caption1" color="secondary">
              {formatRelativeTime(creative.updatedAt, i18n.language)}
            </Text>
            {chips.length > 0 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                {mediaCount > 0 && (
                  <span style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <ImageIcon style={{ width: 12, height: 12, color: 'var(--color-foreground-tertiary)' }} />
                    <Text type="caption2" color="tertiary">
                      {mediaCount}
                    </Text>
                  </span>
                )}
                {buttonCount > 0 && (
                  <span style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <LinkIcon style={{ width: 12, height: 12, color: 'var(--color-foreground-tertiary)' }} />
                    <Text type="caption2" color="tertiary">
                      {buttonCount}
                    </Text>
                  </span>
                )}
              </div>
            )}
          </div>
        }
        chevron
        onClick={onClick}
      />
    </motion.div>
  );
}
