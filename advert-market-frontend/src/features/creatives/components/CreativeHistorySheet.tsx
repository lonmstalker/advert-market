import { Group, GroupItem, Sheet, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { listItem, staggerChildren } from '@/shared/ui/animations';
import type { CreativeVersion } from '../types/creative';

type CreativeHistorySheetProps = {
  open: boolean;
  onClose: () => void;
  versions: CreativeVersion[];
  onSelectVersion?: (version: CreativeVersion) => void;
};

export function CreativeHistorySheet({ open, onClose, versions, onSelectVersion }: CreativeHistorySheetProps) {
  const { t } = useTranslation();

  const HistorySheet = () => (
    <div style={{ padding: 16 }}>
      <Text type="title2" weight="bold">
        {t('creatives.history.title')}
      </Text>
      <motion.div {...staggerChildren} initial="initial" animate="animate" style={{ marginTop: 16 }}>
        <Group>
          {versions.map((v) => (
            <motion.div key={v.version} {...listItem}>
              <GroupItem
                text={`${t('creatives.history.version')} ${v.version}`}
                description={new Date(v.createdAt).toLocaleString()}
                after={
                  <Text type="caption1" color="secondary">
                    {v.draft.text.length > 40 ? `${v.draft.text.slice(0, 40)}...` : v.draft.text}
                  </Text>
                }
                chevron={!!onSelectVersion}
                onClick={onSelectVersion ? () => onSelectVersion(v) : undefined}
              />
            </motion.div>
          ))}
        </Group>
      </motion.div>
    </div>
  );

  return <Sheet sheets={{ history: HistorySheet }} activeSheet="history" opened={open} onClose={onClose} />;
}
