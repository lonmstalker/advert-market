import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import type { ChannelDetail } from '@/features/channels';
import { ChannelOwnerNote } from './ChannelOwnerNote';
import { ChannelRulesSection } from './ChannelRulesSection';

type ChannelRulesTabProps = {
  channel: ChannelDetail;
};

export function ChannelRulesTab({ channel }: ChannelRulesTabProps) {
  const { t } = useTranslation();

  return (
    <motion.div
      key="rules"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.15 }}
      style={{ padding: '0 16px 16px' }}
    >
      {channel.rules ? (
        <>
          <ChannelRulesSection rules={channel.rules} />
          {channel.rules.customRules && <ChannelOwnerNote customRules={channel.rules.customRules} />}
        </>
      ) : (
        <div style={{ padding: '24px 0', textAlign: 'center' }}>
          <Text type="body" color="secondary">
            {t('catalog.channel.noRules')}
          </Text>
        </div>
      )}
    </motion.div>
  );
}
