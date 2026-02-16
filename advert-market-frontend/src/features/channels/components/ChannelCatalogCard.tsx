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
      style={{ cursor: 'pointer', WebkitTapHighlightColor: 'transparent' }}
    >
      <AppSurfaceCard className="am-catalog-card" testId="catalog-channel-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px' }}>
          <ChannelAvatar title={channel.title} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4, minWidth: 0 }}>
              <Text type="body" weight="bold">
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                  {channel.title}
                </span>
              </Text>
              {channel.isVerified && (
                <VerifiedIcon
                  style={{ width: 14, height: 14, color: 'var(--color-accent-primary)', flexShrink: 0 }}
                  aria-label={t('catalog.channel.verified')}
                  role="img"
                />
              )}
              {langs.map((code) => (
                <LanguageBadge key={code} code={code} size="sm" />
              ))}
            </div>
            <Text type="caption1" color="secondary">
              <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {channel.username ? `@${channel.username} · ` : ''}
                {formatCompactNumber(channel.subscriberCount)} {t('catalog.channel.subs')}
              </span>
            </Text>
          </div>
          {channel.pricePerPostNano != null && (
            <div style={{ textAlign: 'right', flexShrink: 0 }}>
              <Text type="callout" weight="bold">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>
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
