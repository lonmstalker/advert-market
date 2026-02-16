import { GroupItem, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { formatCompactNumber } from '@/shared/lib/format-number';
import { formatTon } from '@/shared/lib/ton-format';
import { listItem, pressScale } from '@/shared/ui/animations';
import { ChannelAvatar } from '@/shared/ui/components/channel-avatar';
import type { Channel } from '../types/channel';

type ChannelListItemProps = {
  channel: Channel;
  onClick: () => void;
};

export function ChannelListItem({ channel, onClick }: ChannelListItemProps) {
  const { t } = useTranslation();

  return (
    <motion.div {...listItem} {...pressScale}>
      <GroupItem
        text={channel.title}
        description={`${formatCompactNumber(channel.subscriberCount)} ${t('catalog.channel.subs')}`}
        before={<ChannelAvatar title={channel.title} size="sm" />}
        after={
          channel.pricePerPostNano ? (
            <Text type="callout" weight="medium">
              <span className="am-tabnum">{formatTon(channel.pricePerPostNano)}</span>
            </Text>
          ) : null
        }
        chevron
        onClick={onClick}
      />
    </motion.div>
  );
}
