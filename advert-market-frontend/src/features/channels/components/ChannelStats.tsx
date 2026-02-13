import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { formatLocaleNumber } from '@/shared/lib/format-number';
import type { ChannelDetail } from '../types/channel';

type ChannelStatsProps = {
  channel: ChannelDetail;
};

export function ChannelStats({ channel }: ChannelStatsProps) {
  const { t } = useTranslation();

  return (
    <Group header={t('catalog.channel.stats')}>
      <GroupItem
        text={t('catalog.channel.subscribersStat')}
        after={
          <Text type="body" weight="medium">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatLocaleNumber(channel.subscriberCount)}</span>
          </Text>
        }
      />
      {channel.avgReach != null && (
        <GroupItem
          text={t('catalog.channel.avgReach')}
          after={
            <Text type="body" weight="medium">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatLocaleNumber(channel.avgReach)}</span>
            </Text>
          }
        />
      )}
      {channel.engagementRate != null && (
        <GroupItem
          text={t('catalog.channel.er')}
          after={
            <Text type="body" weight="medium">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{channel.engagementRate.toFixed(1)}%</span>
            </Text>
          }
        />
      )}
    </Group>
  );
}
