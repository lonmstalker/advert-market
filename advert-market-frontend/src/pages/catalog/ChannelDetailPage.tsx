import { useQuery } from '@tanstack/react-query';
import { Spinner, Text } from '@telegram-tools/ui-kit';
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
import {
  ArrowRightIcon,
  ClockIcon,
  EditIcon,
  PostTypeIcon,
  SearchOffIcon,
  ShareIcon,
  TelegramIcon,
  VerifiedIcon,
} from '@/shared/ui/icons';
import type { ChannelDetail, PricingRule } from '@/features/channels';

function formatSubscribers(count: number): string {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`;
  if (count >= 1_000) return `${Math.round(count / 1_000)}K`;
  return String(count);
}

function formatNumber(count: number): string {
  return count.toLocaleString('ru-RU');
}

function erColor(rate: number): string {
  if (rate >= 5) return 'var(--color-state-success)';
  if (rate >= 2) return 'var(--color-foreground-primary)';
  return 'var(--color-state-destructive)';
}

function getChannelLanguages(channel: ChannelDetail): string[] {
  if (channel.languages && channel.languages.length > 0) return channel.languages;
  if (channel.language) return [channel.language];
  return [];
}

function getMinPrice(rules: PricingRule[]): number | null {
  if (rules.length === 0) return null;
  return Math.min(...rules.map((r) => r.priceNano));
}

function formatTimeUntil(isoDate: string): string | null {
  const target = new Date(isoDate);
  const now = new Date();
  const diffMs = target.getTime() - now.getTime();
  if (diffMs <= 0) return null;
  const hours = Math.floor(diffMs / (1000 * 60 * 60));
  const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
  if (hours > 24) {
    const days = Math.floor(hours / 24);
    return `${days}d ${hours % 24}h`;
  }
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

function LanguageBadge({ code }: { code: string }) {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '2px 6px',
        borderRadius: 4,
        background: 'var(--color-background-secondary)',
        border: '1px solid var(--color-border-separator)',
        fontSize: 11,
        fontWeight: 600,
        color: 'var(--color-foreground-secondary)',
        letterSpacing: '0.02em',
        lineHeight: 1.4,
        textTransform: 'uppercase',
        flexShrink: 0,
      }}
    >
      {code}
    </span>
  );
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
          icon={<SearchOffIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
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
  const langs = getChannelLanguages(channel);

  const minPrice = getMinPrice(channel.pricingRules);
  const heroCpm = minPrice != null && channel.avgReach
    ? computeCpm(minPrice, channel.avgReach)
    : null;

  const reachRate = channel.avgReach != null && channel.subscriberCount > 0
    ? (channel.avgReach / channel.subscriberCount * 100)
    : null;

  const nextSlotTime = channel.nextAvailableSlot
    ? formatTimeUntil(channel.nextAvailableSlot)
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
      <div style={{ paddingBottom: 72 }}>
        <motion.div {...fadeIn}>
          {/* Header */}
          <div
            style={{
              padding: '16px 16px 14px',
              background: 'var(--color-background-base)',
              borderBottom: '1px solid var(--color-border-separator)',
            }}
          >
            {/* Row: avatar + title/username + actions */}
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
              <div
                style={{
                  width: 56,
                  height: 56,
                  borderRadius: '50%',
                  background: `hsl(${hue}, 55%, 55%)`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                <span style={{ color: 'var(--color-static-white)', fontSize: 22, fontWeight: 700, lineHeight: 1 }}>{letter}</span>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Text type="title2" weight="bold">
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                      {channel.title}
                    </span>
                  </Text>
                  {channel.isVerified && (
                    <VerifiedIcon
                      style={{ width: 16, height: 16, color: 'var(--color-accent-primary)', flexShrink: 0 }}
                      title={t('catalog.channel.verified')}
                    />
                  )}
                  {langs.map((code) => (
                    <LanguageBadge key={code} code={code} />
                  ))}
                </div>
                {channel.username && (
                  <Text type="subheadline1" color="secondary">
                    <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      @{channel.username}
                    </span>
                  </Text>
                )}
              </div>
              <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                <motion.button
                  {...pressScale}
                  onClick={handleShare}
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 10,
                    border: '1px solid var(--color-border-separator)',
                    background: 'var(--color-background-secondary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    cursor: 'pointer',
                    WebkitTapHighlightColor: 'transparent',
                    padding: 0,
                  }}
                  aria-label={t('catalog.channel.share')}
                >
                  <ShareIcon style={{ width: 16, height: 16, color: 'var(--color-foreground-secondary)' }} />
                </motion.button>
                {isOwner && (
                  <motion.button
                    {...pressScale}
                    type="button"
                    onClick={() => navigate(`/profile/channels/${channel.id}/edit`)}
                    style={{
                      width: 36,
                      height: 36,
                      borderRadius: 10,
                      border: '1px solid var(--color-border-separator)',
                      background: 'var(--color-background-secondary)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      WebkitTapHighlightColor: 'transparent',
                      padding: 0,
                    }}
                    aria-label={t('catalog.channel.edit')}
                  >
                    <EditIcon style={{ width: 16, height: 16, color: 'var(--color-foreground-secondary)' }} />
                  </motion.button>
                )}
              </div>
            </div>

            {/* Topic badges */}
            {channel.topics.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 10 }}>
                {channel.topics.map((topic) => (
                  <span
                    key={topic.slug}
                    style={{
                      padding: '3px 10px',
                      borderRadius: 10,
                      background: 'var(--color-background-secondary)',
                      fontSize: 12,
                      fontWeight: 500,
                      color: 'var(--color-foreground-secondary)',
                    }}
                  >
                    {topic.name}
                  </span>
                ))}
              </div>
            )}
          </div>
        </motion.div>

        {/* Next available slot */}
        {channel.nextAvailableSlot && (
          <motion.div {...slideUp} style={{ padding: '12px 16px 0' }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: '10px 14px',
                borderRadius: 10,
                background: nextSlotTime
                  ? 'var(--color-background-section)'
                  : 'color-mix(in srgb, var(--color-state-success) 8%, transparent)',
                border: nextSlotTime
                  ? '1px solid var(--color-border-separator)'
                  : '1px solid color-mix(in srgb, var(--color-state-success) 20%, transparent)',
              }}
            >
              <ClockIcon
                style={{
                  width: 16,
                  height: 16,
                  color: nextSlotTime ? 'var(--color-foreground-secondary)' : 'var(--color-state-success)',
                  flexShrink: 0,
                }}
              />
              <Text type="caption1" color={nextSlotTime ? 'secondary' : undefined}>
                <span style={{ color: nextSlotTime ? undefined : 'var(--color-state-success)', fontWeight: 500 }}>
                  {nextSlotTime
                    ? t('catalog.channel.nextSlot', { time: nextSlotTime })
                    : t('catalog.channel.nextSlotAvailable')}
                </span>
              </Text>
            </div>
          </motion.div>
        )}

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

        {/* Open in Telegram link */}
        {channel.username && (
          <motion.div {...slideUp} style={{ padding: '0 16px 8px' }}>
            <motion.button
              {...pressScale}
              type="button"
              onClick={handleOpenTelegram}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                padding: '4px 0',
                WebkitTapHighlightColor: 'transparent',
              }}
            >
              <TelegramIcon style={{ width: 16, height: 16, color: 'var(--color-link)' }} />
              <span style={{ fontSize: 14, fontWeight: 500, color: 'var(--color-link)' }}>
                {t('catalog.channel.openInTelegram')}
              </span>
              <ArrowRightIcon style={{ width: 14, height: 14, color: 'var(--color-link)', opacity: 0.6 }} />
            </motion.button>
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
                const localizedType = t(`catalog.channel.postType.${rule.postType}`, { defaultValue: rule.postType });
                const durationLabel = rule.durationHours
                  ? t('catalog.channel.durationHours', { hours: rule.durationHours })
                  : null;
                return (
                  <div key={rule.id} style={pricingCardStyle}>
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10, flex: 1, minWidth: 0 }}>
                      <PostTypeIcon
                        postType={rule.postType}
                        style={{
                          width: 20,
                          height: 20,
                          color: 'var(--color-foreground-secondary)',
                          flexShrink: 0,
                          marginTop: 1,
                        }}
                      />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <Text type="body">
                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                              {localizedType}
                            </span>
                          </Text>
                          {durationLabel && (
                            <span
                              style={{
                                padding: '2px 6px',
                                borderRadius: 6,
                                background: 'var(--color-background-secondary)',
                                fontSize: 11,
                                fontWeight: 500,
                                color: 'var(--color-foreground-secondary)',
                                whiteSpace: 'nowrap',
                                flexShrink: 0,
                              }}
                            >
                              {durationLabel}
                            </span>
                          )}
                        </div>
                        {rule.description && (
                          <Text type="caption1" color="secondary" style={{ marginTop: 2 }}>
                            {rule.description}
                          </Text>
                        )}
                      </div>
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

        {/* Channel rules */}
        <motion.div {...slideUp} style={{ padding: '0 16px 16px' }}>
          <Text type="body" weight="bold" style={{ marginBottom: 12 }}>
            {t('catalog.channel.rules')}
          </Text>
          {channel.rules ? (
            <div
              style={{
                background: 'var(--color-background-base)',
                border: '1px solid var(--color-border-separator)',
                borderRadius: 12,
                overflow: 'hidden',
              }}
            >
              {channel.rules.prohibitedTopics && channel.rules.prohibitedTopics.length > 0 && (
                <div style={ruleRowStyle}>
                  <Text type="caption1" color="secondary">{t('catalog.channel.rulesProhibited')}</Text>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginTop: 4 }}>
                    {channel.rules.prohibitedTopics.map((topic) => (
                      <span
                        key={topic}
                        style={{
                          padding: '2px 8px',
                          borderRadius: 6,
                          background: 'color-mix(in srgb, var(--color-state-destructive) 8%, transparent)',
                          fontSize: 12,
                          fontWeight: 500,
                          color: 'var(--color-state-destructive)',
                        }}
                      >
                        {topic}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              {channel.rules.maxPostChars != null && (
                <div style={ruleRowStyle}>
                  <Text type="caption1" color="secondary">{t('catalog.channel.rulesPostSize')}</Text>
                  <Text type="body">
                    {t('catalog.channel.rulesMaxChars', { count: channel.rules.maxPostChars })}
                  </Text>
                </div>
              )}
              {channel.rules.maxButtons != null && (
                <div style={ruleRowStyle}>
                  <Text type="caption1" color="secondary">{t('catalog.channel.rulesMaxButtons')}</Text>
                  <Text type="body">
                    {t('catalog.channel.rulesButtonsCount', { count: channel.rules.maxButtons })}
                  </Text>
                </div>
              )}
              {channel.rules.customRules && (
                <div style={{ ...ruleRowStyle, borderBottom: 'none' }}>
                  <Text type="caption1" color="secondary" style={{ whiteSpace: 'pre-wrap' }}>
                    {channel.rules.customRules}
                  </Text>
                </div>
              )}
            </div>
          ) : (
            <div
              style={{
                padding: '12px 16px',
                background: 'var(--color-background-section)',
                borderRadius: 12,
              }}
            >
              <Text type="caption1" color="tertiary">
                {t('catalog.channel.noRules')}
              </Text>
            </div>
          )}
        </motion.div>

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
      {!isOwner && (
        <div
          style={{
            position: 'fixed',
            bottom: 0,
            left: 0,
            right: 0,
            padding: '10px 16px',
            background: 'var(--color-background-base)',
            borderTop: '1px solid var(--color-border-separator)',
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            zIndex: 10,
          }}
        >
          {minPrice != null && (
            <div style={{ flexShrink: 0 }}>
              <Text type="callout" weight="bold">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                  {t('catalog.channel.from', { price: formatTon(minPrice) })}
                </span>
              </Text>
            </div>
          )}
          <div style={{ flex: 1 }}>
            <motion.button
              {...pressScale}
              type="button"
              onClick={() => navigate(`/deals/new?channelId=${channel.id}`)}
              style={{
                width: '100%',
                padding: '10px 16px',
                background: 'var(--color-accent-primary)',
                border: 'none',
                borderRadius: 10,
                cursor: 'pointer',
                fontSize: 14,
                fontWeight: 600,
                color: 'var(--color-static-white)',
                WebkitTapHighlightColor: 'transparent',
              }}
            >
              {t('catalog.channel.createDeal')}
            </motion.button>
          </div>
        </div>
      )}
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

const ruleRowStyle: React.CSSProperties = {
  padding: '12px 16px',
  borderBottom: '1px solid var(--color-border-separator)',
};