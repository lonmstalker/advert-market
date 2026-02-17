import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { memo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
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

function formatCategoryLabel(slug: string): string {
  const normalized = slug.replaceAll('-', ' ').replaceAll('_', ' ').trim();
  if (normalized.length === 0) return slug;
  return normalized[0]?.toUpperCase() + normalized.slice(1);
}

function formatEngagementRate(value: number | undefined): string {
  if (value == null || Number.isNaN(value)) return '—';
  const digits = value >= 10 ? 0 : 1;
  return `${value.toFixed(digits)}%`;
}

export const ChannelCatalogCard = memo(function ChannelCatalogCard({ channel, onClick }: ChannelCatalogCardProps) {
  const { t } = useTranslation();
  const haptic = useHaptic();
  const langs = getChannelLanguages(channel);
  const topCategories = channel.categories.slice(0, 3).map(formatCategoryLabel);
  const categories = topCategories.length > 0 ? topCategories : [t('catalog.filters.topicAll')];
  const subscriberCount = formatCompactNumber(channel.subscriberCount);
  const avgViews = channel.avgViews ? formatCompactNumber(channel.avgViews) : '—';
  const er = formatEngagementRate(channel.engagementRate);

  const handleClick = useCallback(() => {
    haptic.impactOccurred('light');
    onClick();
  }, [haptic, onClick]);

  return (
    <motion.div
      {...listItem}
      {...pressScale}
      onClick={handleClick}
      className="cursor-pointer [-webkit-tap-highlight-color:transparent]"
    >
      <AppSurfaceCard className="am-catalog-card am-channel-card" testId="catalog-channel-card">
        <div className="am-channel-card__header">
          <ChannelAvatar title={channel.title} />
          <div className="am-channel-card__identity">
            <div className="am-channel-card__title-row">
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
            {channel.username && (
              <Text type="caption1" color="secondary">
                <span className="block truncate">@{channel.username}</span>
              </Text>
            )}
          </div>
          {channel.pricePerPostNano != null && (
            <div className="am-channel-card__price">
              <Text type="subheadline2" weight="bold">
                <span className="tabular-nums">
                  {t('catalog.channel.from', { price: `${formatTonCompact(channel.pricePerPostNano)} TON` })}
                </span>
              </Text>
            </div>
          )}
        </div>

        <div className="am-channel-card__categories">
          {categories.map((category) => (
            <Text key={category} type="caption2" weight="bold" color="secondary">
              <span className="am-channel-card__chip">{category}</span>
            </Text>
          ))}
        </div>

        <div className="am-channel-card__metrics">
          <div className="am-channel-card__metric">
            <Text type="subheadline1" weight="bold">
              <span className="am-tabnum">{subscriberCount}</span>
            </Text>
            <Text type="caption2" color="secondary">
              {t('catalog.channel.subs')}
            </Text>
          </div>
          <div className="am-channel-card__metric">
            <Text type="subheadline2" weight="medium">
              <span className="am-tabnum">{avgViews}</span>
            </Text>
            <Text type="caption2" color="secondary">
              {t('catalog.channel.views')}
            </Text>
          </div>
          <div className="am-channel-card__metric">
            <Text type="subheadline2" weight="medium">
              <span className="am-tabnum">{er}</span>
            </Text>
            <Text type="caption2" color="secondary">
              ER
            </Text>
          </div>
        </div>
      </AppSurfaceCard>
    </motion.div>
  );
});
