import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import type { ChannelDetail } from '@/features/channels';
import { slideUp } from '@/shared/ui/animations';
import { ChannelDetailStats } from './ChannelDetailStats';
import { ChannelOpenTelegram } from './ChannelOpenTelegram';

type ChannelAboutTabProps = {
  channel: ChannelDetail;
  telegramLink: string | null;
};

export function ChannelAboutTab({ channel, telegramLink }: ChannelAboutTabProps) {
  return (
    <motion.div
      key="about"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.15 }}
    >
      {channel.description && (
        <motion.div {...slideUp} style={{ padding: '0 16px 16px' }}>
          <div style={{ whiteSpace: 'pre-wrap' }}>
            <Text type="subheadline1" color="secondary">
              {channel.description}
            </Text>
          </div>
        </motion.div>
      )}

      <ChannelDetailStats channel={channel} />

      {telegramLink && <ChannelOpenTelegram link={telegramLink} username={channel.username} />}
    </motion.div>
  );
}
