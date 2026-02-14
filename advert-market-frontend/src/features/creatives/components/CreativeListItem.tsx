import { GroupItem, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { pressScale } from '@/shared/ui/animations';
import type { CreativeTemplate } from '../types/creative';

type CreativeListItemProps = {
  creative: CreativeTemplate;
  onClick: () => void;
};

export function CreativeListItem({ creative, onClick }: CreativeListItemProps) {
  const { t } = useTranslation();
  const mediaCount = creative.draft.media.length;
  const textPreview = creative.draft.text.length > 60 ? `${creative.draft.text.slice(0, 60)}...` : creative.draft.text;

  return (
    <motion.div {...pressScale}>
      <GroupItem
        text={creative.title}
        description={textPreview}
        after={
          mediaCount > 0 ? (
            <Text type="caption1" color="secondary">
              {t('creatives.mediaCount', { count: mediaCount })}
            </Text>
          ) : undefined
        }
        chevron
        onClick={onClick}
      />
    </motion.div>
  );
}
