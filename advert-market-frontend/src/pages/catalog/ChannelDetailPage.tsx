import { useQuery } from '@tanstack/react-query';
import { Button, Spinner, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { fetchChannelDetail, useChannelRights } from '@/features/channels';
import { channelKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { copyToClipboard } from '@/shared/lib/clipboard';
import { computeCpm, formatCpm, formatTon } from '@/shared/lib/ton-format';
import { BackButtonHandler, EmptyState } from '@/shared/ui';
import { fadeIn, pressScale, slideUp } from '@/shared/ui/animations';
import type { PricingRule } from '@/features/channels';

function formatSubscribers(count: number): string {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`;
  if (count >= 1_000) return `${Math.round(count / 1_000)}K`;
  return String(count);
}

function formatNumber(count: number): string {
  return count.toLocaleString('ru-RU');
}

function erColor(rate: number): string {
  if (rate > 5) return 'var(--color-accent-primary)';
  if (rate >= 2) return 'var(--color-foreground-secondary)';
  return 'var(--color-foreground-tertiary)';
}

const POST_TYPE_ICONS: Record<string, string> = {
  NATIVE: '\u{1F4DD}',
  STORY: '\u{1F4F1}',
  REPOST: '\u{1F504}',
  FORWARD: '\u{27A1}\uFE0F',
  INTEGRATION: '\u{1F91D}',
  REVIEW: '\u{2B50}',
  MENTION: '\u{1F4AC}',
  GIVEAWAY: '\u{1F381}',
  PINNED: '\u{1F4CC}',
  POLL: '\u{1F4CA}',
};

function getPostTypeIcon(postType: string): string {
  return POST_TYPE_ICONS[postType.toUpperCase()] ?? '\u{1F4E2}';
}

function getMinPrice(rules: PricingRule[]): number | null {
  if (rules.length === 0) return null;
  return Math.min(...rules.map((r) => r.priceNano));
}

export default function ChannelDetailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { channelId } = useParams<{ channelId: string }>();
  const haptic = useHaptic();
  const { showSuccess } = useToast();
  const id = Number(channelId);

  const {
    data: channel,
    isLoading,
    isError,
  } = useQuery({
    queryKey: channelKeys.detail(id),
    queryFn: () => fetchChannelDetail(id),
    enabled: !Number.isNaN(id),
  });

  const { isOwner, isLoading: rightsLoading } = useChannelRights(id);

  if (isLoading || rightsLoading) {
    return (
      <>
        <BackButtonHandler />
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
          <Spinner size="32px" color="accent" />
        </div>
      </>
    );
  }

  if (isError || !channel) {
    return (
      <>
        <BackButtonHandler />
        <EmptyState
          emoji="\u{1F614}"
          title={t('errors.notFound')}
          description={t('catalog.empty.description')}
          actionLabel={t('common.back')}
          onAction={() => navigate('/catalog')}
        />
      </>
    );
  }

  const letter = channel.title.charAt(0).toUpperCase();
  const hue = (channel.title.charCodeAt(0) * 37 + (channel.title.charCodeAt(1) || 0) * 53) % 360;

  const minPrice = getMinPrice(channel.pricingRules);
  const heroCpm = minPrice != null && channel.avgReach
    ? computeCpm(minPrice, channel.avgReach)
    : null;

  const reachRate = channel.avgReach != null && channel.subscriberCount > 0
    ? (channel.avgReach / channel.subscriberCount * 100)
    : null;

  const handleShare = async () => {
    const link = `https://t.me/AdvertMarketBot/app?startapp=channel_${channel.id}`;
    const ok = await copyToClipboard(link);
    if (ok) {
      haptic.notificationOccurred('success');
      showSuccess(t('catalog.channel.share'));
    }
  };

  const handleOpenTelegram = () => {
    if (channel.username) {
      window.open(`https://t.me/${channel.username}`, '_blank');
    }
  };

  return (
    <>
      <BackButtonHandler />
      <div style={{ paddingBottom: 88 }}>
        <motion.div {...fadeIn}>
          {/* Header */}
          <div
            style={{
              padding: '24px 16px 20px',
              background: 'var(--color-background-base)',
              borderBottom: '1px solid var(--color-border-separator)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 12,
              textAlign: 'center',
            }}
          >
            <div
              style={{
                width: 72,
                height: 72,
                borderRadius: '50%',
                background: `hsl(${hue}, 55%, 55%)`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <span style={{ color: 'var(--color-static-white)', fontSize: 28, fontWeight: 700, lineHeight: 1 }}>{letter}</span>
            </div>
            <div>
              <Text type="title1" weight="bold">
                {channel.title}
              </Text>
              {channel.username && (
                <Text type="subheadline1" color="secondary">
                  @{channel.username}
                </Text>
              )}
            </div>

            {/* Topic badges */}
            {channel.topics.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, justifyContent: 'center' }}>
                {channel.topics.map((topic) => (
                  <span
                    key={topic.slug}
                    style={{
                      padding: '4px 12px',
                      borderRadius: 12,
                      background: 'var(--color-background-secondary)',
                      fontSize: 13,
                      fontWeight: 500,
                      color: 'var(--color-foreground-secondary)',
                    }}
                  >
                    {topic.name}
                  </span>
                ))}
              </div>
            )}

            {/* Share button */}
            <motion.button
              {...pressScale}
              onClick={handleShare}
              style={{
                background: 'var(--color-background-secondary)',
                border: 'none',
                borderRadius: 20,
                padding: '6px 16px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                WebkitTapHighlightColor: 'transparent',
              }}
              aria-label={t('catalog.channel.share')}
            >
              <span style={{ fontSize: 14 }}>{'\u{1F4E4}'}</span>
              <Text type="caption1" color="link">
                {t('catalog.channel.share')}
              </Text>
            </motion.button>
          </div>
        </motion.div>

        {/* Hero CPM section */}
        {heroCpm != null && (
          <motion.div {...slideUp} style={{ padding: '20px 16px 4px', textAlign: 'center' }}>
            <Text type="title1" weight="bold">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                {formatCpm(heroCpm)} TON / 1K {t('catalog.channel.views')}
              </span>
            </Text>
          </motion.div>
        )}

        {/* Stats row */}
        <motion.div {...slideUp} style={{ padding: '16px 16px 0', display: 'flex', gap: 8 }}>
          <div style={statCardStyle}>
            <Text type="title2" weight="bold">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatNumber(channel.subscriberCount)}</span>
            </Text>
            <Text type="caption1" color="secondary">
              {t('catalog.channel.subscribersStat')}
            </Text>
          </div>
          {channel.avgReach != null && (
            <div style={statCardStyle}>
              <Text type="title2" weight="bold">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatSubscribers(channel.avgReach)}</span>
              </Text>
              <Text type="caption1" color="secondary">
                {t('catalog.channel.avgReach')}
              </Text>
              {reachRate != null && (
                <Text type="caption1" color="tertiary">
                  <span style={{ fontVariantNumeric: 'tabular-nums' }}>{reachRate.toFixed(0)}% reach</span>
                </Text>
              )}
            </div>
          )}
          {channel.engagementRate != null && (
            <div style={statCardStyle}>
              <Text type="title2" weight="bold">
                <span style={{ fontVariantNumeric: 'tabular-nums', color: erColor(channel.engagementRate) }}>
                  {channel.engagementRate.toFixed(1)}%
                </span>
              </Text>
              <Text type="caption1" color="secondary">
                {t('catalog.channel.er')}
              </Text>
            </div>
          )}
        </motion.div>

        {/* Description */}
        {channel.description && (
          <motion.div {...slideUp} style={{ padding: 16 }}>
            <Text type="subheadline1" color="secondary" style={{ whiteSpace: 'pre-wrap' }}>
              {channel.description}
            </Text>
          </motion.div>
        )}

        {/* Open in Telegram button */}
        {channel.username && (
          <motion.div {...slideUp} style={{ padding: '0 16px 16px' }}>
            <button
              type="button"
              onClick={handleOpenTelegram}
              style={{
                width: '100%',
                padding: '12px 16px',
                background: 'var(--color-background-base)',
                border: '1px solid var(--color-border-separator)',
                borderRadius: 12,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8,
                WebkitTapHighlightColor: 'transparent',
              }}
            >
              <span style={{ fontSize: 16 }}>{'\u{1F4F1}'}</span>
              <Text type="body" color="link">
                {t('catalog.channel.openInTelegram')}
              </Text>
              <span style={{ fontSize: 12, color: 'var(--color-foreground-tertiary)' }}>{'\u{2192}'}</span>
            </button>
          </motion.div>
        )}

        {/* Pricing table */}
        {channel.pricingRules.length > 0 && (
          <motion.div {...slideUp} style={{ padding: '0 16px 16px' }}>
            <Text type="body" weight="bold" style={{ marginBottom: 12 }}>
              {t('catalog.channel.pricing')}
            </Text>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {channel.pricingRules.map((rule) => {
                const ruleCpm = channel.avgReach ? computeCpm(rule.priceNano, channel.avgReach) : null;
                return (
                  <div key={rule.id} style={pricingCardStyle}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: 1, minWidth: 0 }}>
                      <span style={{ fontSize: 18, flexShrink: 0 }}>{getPostTypeIcon(rule.postType)}</span>
                      <Text type="body">
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                          {rule.postType}
                        </span>
                      </Text>
                    </div>
                    <div style={{ textAlign: 'right', flexShrink: 0 }}>
                      <Text type="callout" weight="bold">
                        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(rule.priceNano)}</span>
                      </Text>
                      {ruleCpm != null && (
                        <Text type="caption1" color="secondary">
                          <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                            {t('catalog.channel.cpmShort', { value: formatCpm(ruleCpm) })}
                          </span>
                        </Text>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </motion.div>
        )}

        {/* Social proof */}
        <motion.div {...slideUp} style={{ padding: '0 16px 16px' }}>
          <div
            style={{
              padding: '12px 16px',
              background: 'var(--color-background-section)',
              borderRadius: 12,
              textAlign: 'center',
            }}
          >
            <Text type="caption1" color="secondary">
              {t('catalog.channel.newChannel')}
            </Text>
          </div>
        </motion.div>
      </div>

      {/* Sticky bottom CTA */}
      <div
        style={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          padding: 16,
          background: 'var(--color-background-base)',
          borderTop: '1px solid var(--color-border-separator)',
          display: 'flex',
          gap: 12,
          zIndex: 10,
        }}
      >
        <div style={{ flex: 1 }}>
          <motion.div {...pressScale}>
            {isOwner ? (
              <Button
                text={t('catalog.channel.edit')}
                type="primary"
                onClick={() => navigate(`/profile/channels/${channel.id}/edit`)}
              />
            ) : (
              <Button
                text={t('catalog.channel.createDeal')}
                type="primary"
                onClick={() => navigate(`/deals/new?channelId=${channel.id}`)}
              />
            )}
          </motion.div>
        </div>
      </div>
    </>
  );
}

const statCardStyle: React.CSSProperties = {
  flex: 1,
  background: 'var(--color-background-base)',
  border: '1px solid var(--color-border-separator)',
  borderRadius: 12,
  padding: 12,
  textAlign: 'center',
};

const pricingCardStyle: React.CSSProperties = {
  background: 'var(--color-background-base)',
  border: '1px solid var(--color-border-separator)',
  borderRadius: 12,
  padding: 16,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 12,
};
