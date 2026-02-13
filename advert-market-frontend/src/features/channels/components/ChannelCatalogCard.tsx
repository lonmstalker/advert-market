import { useQuery } from '@tanstack/react-query';
import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { memo } from 'react';
import { useTranslation } from 'react-i18next';
import { channelKeys } from '@/shared/api/query-keys';
import { getChannelLanguages } from '@/shared/lib/channel-utils';
import { engagementRateColor } from '@/shared/lib/engagement-rate';
import { formatCompactNumber } from '@/shared/lib/format-number';
import { computeCpm, formatCpm, formatTonCompact } from '@/shared/lib/ton-format';
import { listItem, pressScale } from '@/shared/ui/animations';
import { ChannelAvatar } from '@/shared/ui/components/channel-avatar';
import { LanguageBadge } from '@/shared/ui/components/language-badge';
import { VerifiedIcon } from '@/shared/ui/icons';
import { fetchCategories } from '../api/channels';
import type { Category, Channel } from '../types/channel';

type ChannelCatalogCardProps = {
  channel: Channel;
  onClick: () => void;
};

function MetricPill({ value, label, valueColor }: { value: string; label: string; valueColor?: string }) {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4,
        padding: '3px 8px',
        borderRadius: 8,
        background: 'var(--color-background-secondary)',
        fontSize: 12,
        lineHeight: 1.2,
        whiteSpace: 'nowrap',
      }}
    >
      <span
        style={{
          fontWeight: 600,
          color: valueColor ?? 'var(--color-foreground-primary)',
          fontVariantNumeric: 'tabular-nums',
        }}
      >
        {value}
      </span>
      <span style={{ color: 'var(--color-foreground-tertiary)', fontWeight: 400 }}>{label}</span>
    </span>
  );
}

function CategoryBadge({ slug, categories }: { slug: string; categories: Category[] }) {
  const { i18n } = useTranslation();
  const lang = i18n.language;
  const cat = categories.find((c) => c.slug === slug);
  const label = cat ? (cat.localizedName[lang] ?? cat.localizedName.ru ?? cat.slug) : slug;

  return (
    <span
      style={{
        padding: '2px 8px',
        borderRadius: 8,
        background: 'var(--color-background-secondary)',
        fontSize: 11,
        fontWeight: 500,
        color: 'var(--color-foreground-secondary)',
        whiteSpace: 'nowrap',
      }}
    >
      {label}
    </span>
  );
}

export const ChannelCatalogCard = memo(function ChannelCatalogCard({ channel, onClick }: ChannelCatalogCardProps) {
  const { t } = useTranslation();

  const { data: categories = [] } = useQuery({
    queryKey: channelKeys.categories(),
    queryFn: fetchCategories,
    staleTime: Number.POSITIVE_INFINITY,
  });

  const cpm =
    channel.pricePerPostNano != null && channel.avgViews
      ? computeCpm(channel.pricePerPostNano, channel.avgViews)
      : null;

  const langs = getChannelLanguages(channel);

  return (
    <motion.div
      {...listItem}
      {...pressScale}
      onClick={onClick}
      style={{
        background: 'var(--color-background-base)',
        border: '1px solid var(--color-border-separator)',
        borderRadius: 16,
        padding: 16,
        cursor: 'pointer',
        WebkitTapHighlightColor: 'transparent',
      }}
    >
      {/* Row 1: avatar + title/username + price/CPM */}
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
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
                title={t('catalog.channel.verified')}
              />
            )}
            {langs.map((code) => (
              <LanguageBadge key={code} code={code} size="sm" />
            ))}
          </div>
          {channel.username && (
            <Text type="caption1" color="secondary">
              <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                @{channel.username}
              </span>
            </Text>
          )}
        </div>
        {channel.pricePerPostNano != null && (
          <div style={{ textAlign: 'right', flexShrink: 0 }}>
            <Text type="callout" weight="bold">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                {t('catalog.channel.from', { price: `${formatTonCompact(channel.pricePerPostNano)} TON` })}
              </span>
            </Text>
            {cpm != null && (
              <Text type="caption1" color="secondary">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                  {t('catalog.channel.cpmShort', { value: formatCpm(cpm) })}
                </span>
              </Text>
            )}
          </div>
        )}
      </div>

      {/* Row 2: metric pills */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 10, flexWrap: 'wrap' }}>
        <MetricPill value={formatCompactNumber(channel.subscriberCount)} label={t('catalog.channel.subs')} />
        {channel.avgViews != null && (
          <MetricPill value={formatCompactNumber(channel.avgViews)} label={t('catalog.channel.reach')} />
        )}
        {channel.engagementRate != null && (
          <MetricPill
            value={`${channel.engagementRate.toFixed(1)}%`}
            label="ER"
            valueColor={engagementRateColor(channel.engagementRate)}
          />
        )}
      </div>

      {/* Row 3: category badges */}
      {channel.categories.length > 0 && categories.length > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 8, flexWrap: 'wrap' }}>
          {channel.categories.map((slug) => (
            <CategoryBadge key={slug} slug={slug} categories={categories} />
          ))}
        </div>
      )}
    </motion.div>
  );
});
