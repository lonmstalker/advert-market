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
import { formatTon } from '@/shared/lib/ton-format';
import { BackButtonHandler, EmptyState } from '@/shared/ui';
import { fadeIn, pressScale, slideUp } from '@/shared/ui/animations';

function formatSubscribers(count: number): string {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`;
  if (count >= 1_000) return `${Math.round(count / 1_000)}K`;
  return String(count);
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
          emoji="😔"
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

  const handleShare = async () => {
    const link = `https://t.me/AdvertMarketBot/app?startapp=channel_${channel.id}`;
    const ok = await copyToClipboard(link);
    if (ok) {
      haptic.notificationOccurred('success');
      showSuccess(t('catalog.channel.share'));
    }
  };

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
              <span style={{ color: '#fff', fontSize: 28, fontWeight: 700, lineHeight: 1 }}>{letter}</span>
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
              <span style={{ fontSize: 14 }}>📤</span>
              <Text type="caption1" color="link">
                {t('catalog.channel.share')}
              </Text>
            </motion.button>
          </div>
        </motion.div>

        {/* Quick stats row — 3 visual cards */}
        <motion.div {...slideUp} style={{ padding: '16px 16px 0', display: 'flex', gap: 8 }}>
          <div style={statCardStyle}>
            <Text type="title2" weight="bold">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatSubscribers(channel.subscriberCount)}</span>
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
            </div>
          )}
          {channel.engagementRate != null && (
            <div style={statCardStyle}>
              <Text type="title2" weight="bold">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{channel.engagementRate.toFixed(1)}%</span>
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

        {/* Pricing cards */}
        {channel.pricingRules.length > 0 && (
          <motion.div {...slideUp} style={{ padding: '0 16px 16px' }}>
            <Text type="body" weight="bold" style={{ marginBottom: 12 }}>
              {t('catalog.channel.pricing')}
            </Text>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {channel.pricingRules.map((rule) => (
                <div key={rule.id} style={pricingCardStyle}>
                  <Text type="body">{rule.postType}</Text>
                  <Text type="callout" weight="bold">
                    <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(rule.priceNano)}</span>
                  </Text>
                </div>
              ))}
            </div>
          </motion.div>
        )}
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
