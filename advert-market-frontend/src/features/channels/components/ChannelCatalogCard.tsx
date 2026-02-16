import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { memo } from 'react';
import { useTranslation } from 'react-i18next';
import { getChannelLanguages } from '@/shared/lib/channel-utils';
import { formatCompactNumber } from '@/shared/lib/format-number';
import { formatTonCompact } from '@/shared/lib/ton-format';
import { AppSurfaceCard } from '@/shared/ui';
import { listItem, pressScale } from '@/shared/ui/animations';
import { ChannelAvatar } from '@/shared/ui/components/channel-avatar';
import { LanguageBadge } from '@/shared/ui/components/language-badge';
import { VerifiedIcon } from '@/shared/ui/icons';
import type { Channel } from '../types/channel';

type ChannelCatalogCardProps = {
  channel: Channel;
  onClick: () => void;
};

export const ChannelCatalogCard = memo(function ChannelCatalogCard({ channel, onClick }: ChannelCatalogCardProps) {
  const { t } = useTranslation();
  const langs = getChannelLanguages(channel);

  return (
    <motion.div
      {...listItem}
      {...pressScale}
      onClick={onClick}
      className="cursor-pointer [-webkit-tap-highlight-color:transparent]"
    >
      <AppSurfaceCard className="am-catalog-card" testId="catalog-channel-card">
        <div className="flex items-center gap-3 px-4 py-4">
          <ChannelAvatar title={channel.title} />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1 min-w-0">
              <Text type="body" weight="bold">
                <span className="block truncate">{channel.title}</span>
              </Text>
              {channel.isVerified && (
                <VerifiedIcon
                  className="size-3.5 text-accent shrink-0"
                  aria-label={t('catalog.channel.verified')}
                  role="img"
                />
              )}
              {langs.map((code) => (
                <LanguageBadge key={code} code={code} size="sm" />
              ))}
            </div>
            <Text type="caption1" color="secondary">
              <span className="block truncate">
                {channel.username ? `@${channel.username} · ` : ''}
                {formatCompactNumber(channel.subscriberCount)} {t('catalog.channel.subs')}
              </span>
            </Text>
          </div>
          {channel.pricePerPostNano != null && (
            <div className="text-right shrink-0">
              <Text type="callout" weight="bold">
                <span className="tabular-nums">
                  {t('catalog.channel.from', { price: `${formatTonCompact(channel.pricePerPostNano)} TON` })}
                </span>
              </Text>
            </div>
          )}
        </div>
      </AppSurfaceCard>
    </motion.div>
  );
});
