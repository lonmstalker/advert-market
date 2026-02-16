import { GroupItem, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { formatRelativeTime } from '@/shared/lib/date-format';
import { pressScale } from '@/shared/ui/animations';
import { ImageIcon, LinkIcon } from '@/shared/ui/icons';
import type { CreativeTemplate } from '../types/creative';
import { countButtons } from '../types/creative';

type CreativeListItemProps = {
  creative: CreativeTemplate;
  onClick: () => void;
};

export function CreativeListItem({ creative, onClick }: CreativeListItemProps) {
  const { t, i18n } = useTranslation();
  const haptic = useHaptic();
  const mediaCount = creative.draft.media.length;
  const buttonCount = countButtons(creative.draft.buttons);
  const textPreview = creative.draft.text.length > 60 ? `${creative.draft.text.slice(0, 60)}...` : creative.draft.text;

  const chips: string[] = [];
  if (mediaCount > 0) chips.push(t('creatives.mediaCount', { count: mediaCount }));
  if (buttonCount > 0) chips.push(t('creatives.buttonsCount', { count: buttonCount }));

  const handleClick = useCallback(() => {
    haptic.impactOccurred('light');
    onClick();
  }, [haptic, onClick]);

  return (
    <motion.div {...pressScale}>
      <GroupItem
        text={creative.title}
        description={textPreview}
        after={
          <div className="flex flex-col items-end gap-0.5">
            <Text type="caption1" color="secondary">
              {formatRelativeTime(creative.updatedAt, i18n.language)}
            </Text>
            {chips.length > 0 && (
              <div className="flex items-center gap-1.5">
                {mediaCount > 0 && (
                  <span className="flex items-center gap-0.5">
                    <ImageIcon className="size-3 text-fg-tertiary" />
                    <Text type="caption2" color="tertiary">
                      {mediaCount}
                    </Text>
                  </span>
                )}
                {buttonCount > 0 && (
                  <span className="flex items-center gap-0.5">
                    <LinkIcon className="size-3 text-fg-tertiary" />
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
        onClick={handleClick}
      />
    </motion.div>
  );
}
