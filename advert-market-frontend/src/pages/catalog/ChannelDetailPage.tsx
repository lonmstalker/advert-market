import { useQuery } from '@tanstack/react-query';
import { Spinner, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import type { ChannelDetail, PricingRule } from '@/features/channels';
import { fetchChannelDetail, useChannelRights } from '@/features/channels';
import { channelKeys } from '@/shared/api/query-keys';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { useToast } from '@/shared/hooks/use-toast';
import { copyToClipboard } from '@/shared/lib/clipboard';
import { formatFiat } from '@/shared/lib/fiat-format';
import { computeCpm, formatCpm, formatTon } from '@/shared/lib/ton-format';
import { BackButtonHandler, EmptyState, Popover, SegmentControl } from '@/shared/ui';
import { fadeIn, pressScale, slideUp } from '@/shared/ui/animations';
import {
  ArrowRightIcon,
  ClockIcon,
  EditIcon,
  InfoIcon,
  PostTypeIcon,
  SearchOffIcon,
  ShareIcon,
  TelegramIcon,
  VerifiedIcon,
} from '@/shared/ui/icons';

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

function getOverlapLabel(
  rule: PricingRule,
  channelFreq: number | undefined,
  t: (key: string, opts?: Record<string, unknown>) => string,
): { label: string; hasTooltip: boolean; freq?: number; dur?: number } | null {
  const freq = rule.postFrequencyHours ?? channelFreq;
  const dur = rule.durationHours;
  if (freq && dur) return { label: t('catalog.channel.overlapFormat', { freq, dur }), hasTooltip: true, freq, dur };
  if (dur) return { label: t('catalog.channel.onlyDuration', { dur }), hasTooltip: false };
  if (freq) return { label: t('catalog.channel.onlyFrequency', { freq }), hasTooltip: false };
  return null;
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

function formatChannelAge(createdAt: string, t: (key: string, opts?: Record<string, unknown>) => string): string {
  const created = new Date(createdAt);
  const now = new Date();
  const diffMs = now.getTime() - created.getTime();
  const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (days < 1) return t('catalog.channel.addedToday');
  if (days < 30) return t('catalog.channel.addedDaysAgo', { count: days });
  const months = Math.floor(days / 30);
  if (months < 12) return t('catalog.channel.addedMonthsAgo', { count: months });
  const monthNames = [
    'января',
    'февраля',
    'марта',
    'апреля',
    'мая',
    'июня',
    'июля',
    'августа',
    'сентября',
    'октября',
    'ноября',
    'декабря',
  ];
  return t('catalog.channel.onPlatformSince', {
    date: `${monthNames[created.getMonth()]} ${created.getFullYear()}`,
  });
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

function RuleIndicator({ allowed }: { allowed: boolean }) {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: 18,
        height: 18,
        borderRadius: '50%',
        background: allowed
          ? 'color-mix(in srgb, var(--color-state-success) 12%, transparent)'
          : 'color-mix(in srgb, var(--color-state-destructive) 12%, transparent)',
        flexShrink: 0,
        marginTop: 1,
      }}
    >
      <svg width="10" height="10" viewBox="0 0 10 10" fill="none" aria-hidden="true">
        {allowed ? (
          <path
            d="M2 5.5L4 7.5L8 3"
            stroke="var(--color-state-success)"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        ) : (
          <path
            d="M3 3L7 7M7 3L3 7"
            stroke="var(--color-state-destructive)"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}
      </svg>
    </span>
  );
}

function ChannelRulesSection({
  rules,
  t,
}: {
  rules: NonNullable<ChannelDetail['rules']>;
  t: (key: string, opts?: Record<string, unknown>) => string;
}) {
  const hasContent = rules.maxPostChars != null || (rules.prohibitedTopics && rules.prohibitedTopics.length > 0);
  const hasMedia = rules.mediaAllowed != null || rules.mediaTypes != null || rules.maxMediaCount != null;
  const hasLinksButtons = rules.linksAllowed != null || rules.mentionsAllowed != null || rules.maxButtons != null;
  const hasFormatting = rules.formattingAllowed != null;
  const sections: { label: string; rows: React.ReactNode[] }[] = [];

  if (hasContent) {
    const rows: React.ReactNode[] = [];
    if (rules.maxPostChars != null) {
      rows.push(
        <div key="chars" style={ruleItemStyle}>
          <RuleIndicator allowed />
          <Text type="caption1">{t('catalog.channel.rulesMaxChars', { count: rules.maxPostChars })}</Text>
        </div>,
      );
    }
    if (rules.prohibitedTopics && rules.prohibitedTopics.length > 0) {
      rows.push(
        <div key="prohibited" style={{ ...ruleItemStyle, flexWrap: 'wrap' }}>
          <RuleIndicator allowed={false} />
          <Text type="caption1" color="secondary">
            {t('catalog.channel.rulesProhibited')}:
          </Text>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, width: '100%', paddingLeft: 26 }}>
            {rules.prohibitedTopics.map((topic) => (
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
        </div>,
      );
    }
    sections.push({ label: t('catalog.channel.rulesPostSize'), rows });
  }

  if (hasMedia) {
    const rows: React.ReactNode[] = [];
    if (rules.mediaAllowed != null) {
      rows.push(
        <div key="media" style={ruleItemStyle}>
          <RuleIndicator allowed={rules.mediaAllowed} />
          <Text type="caption1">
            {rules.mediaAllowed ? t('catalog.channel.rulesMediaAllowed') : t('catalog.channel.rulesMediaNotAllowed')}
          </Text>
        </div>,
      );
    }
    if (rules.mediaTypes && rules.mediaTypes.length > 0) {
      const types = rules.mediaTypes.map((mt) => t(`catalog.channel.mediaType.${mt}`, { defaultValue: mt })).join(', ');
      rows.push(
        <div key="types" style={ruleItemStyle}>
          <RuleIndicator allowed />
          <Text type="caption1">{t('catalog.channel.rulesMediaTypes', { types })}</Text>
        </div>,
      );
    }
    if (rules.maxMediaCount != null) {
      rows.push(
        <div key="count" style={ruleItemStyle}>
          <RuleIndicator allowed />
          <Text type="caption1">{t('catalog.channel.rulesMaxMedia', { count: rules.maxMediaCount })}</Text>
        </div>,
      );
    }
    sections.push({ label: t('catalog.channel.rulesMedia'), rows });
  }

  if (hasLinksButtons) {
    const rows: React.ReactNode[] = [];
    if (rules.linksAllowed != null) {
      rows.push(
        <div key="links" style={ruleItemStyle}>
          <RuleIndicator allowed={rules.linksAllowed} />
          <Text type="caption1">
            {rules.linksAllowed ? t('catalog.channel.rulesLinksAllowed') : t('catalog.channel.rulesLinksNotAllowed')}
          </Text>
        </div>,
      );
    }
    if (rules.mentionsAllowed != null) {
      rows.push(
        <div key="mentions" style={ruleItemStyle}>
          <RuleIndicator allowed={rules.mentionsAllowed} />
          <Text type="caption1">
            {rules.mentionsAllowed
              ? t('catalog.channel.rulesMentionsAllowed')
              : t('catalog.channel.rulesMentionsNotAllowed')}
          </Text>
        </div>,
      );
    }
    if (rules.maxButtons != null) {
      rows.push(
        <div key="buttons" style={ruleItemStyle}>
          <RuleIndicator allowed />
          <Text type="caption1">
            {t('catalog.channel.rulesMaxButtons')}:{' '}
            {t('catalog.channel.rulesButtonsCount', { count: rules.maxButtons })}
          </Text>
        </div>,
      );
    }
    sections.push({ label: t('catalog.channel.rulesLinks'), rows });
  }

  if (hasFormatting) {
    sections.push({
      label: t('catalog.channel.rulesFormatting'),
      rows: [
        <div key="fmt" style={ruleItemStyle}>
          <RuleIndicator allowed={rules.formattingAllowed as boolean} />
          <Text type="caption1">
            {rules.formattingAllowed
              ? t('catalog.channel.rulesFormattingAllowed')
              : t('catalog.channel.rulesFormattingNotAllowed')}
          </Text>
        </div>,
      ],
    });
  }

  if (sections.length === 0) {
    return (
      <div style={{ padding: '12px 16px', background: 'var(--color-background-section)', borderRadius: 12 }}>
        <Text type="caption1" color="tertiary">
          {t('catalog.channel.noRules')}
        </Text>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {sections.length > 0 && (
        <div
          style={{
            background: 'var(--color-background-base)',
            border: '1px solid var(--color-border-separator)',
            borderRadius: 12,
            overflow: 'hidden',
          }}
        >
          {sections.map((section, i) => (
            <div
              key={section.label}
              style={{
                padding: '12px 16px',
                borderBottom: i < sections.length - 1 ? '1px solid var(--color-border-separator)' : 'none',
              }}
            >
              <Text type="caption1" weight="medium" color="secondary" style={{ marginBottom: 8 }}>
                {section.label}
              </Text>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>{section.rows}</div>
            </div>
          ))}
        </div>
      )}
    </div>
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
  const [activeTab, setActiveTab] = useState<'pricing' | 'conditions'>('pricing');

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
  const heroCpm = minPrice != null && channel.avgReach ? computeCpm(minPrice, channel.avgReach) : null;

  const reachRate =
    channel.avgReach != null && channel.subscriberCount > 0 ? (channel.avgReach / channel.subscriberCount) * 100 : null;

  const nextSlotTime = channel.nextAvailableSlot ? formatTimeUntil(channel.nextAvailableSlot) : null;

  const handleShare = async () => {
    const link = `https://t.me/AdvertMarketBot/app?startapp=channel_${channel.id}`;
    const ok = await copyToClipboard(link);
    if (ok) {
      haptic.notificationOccurred('success');
      showSuccess(t('catalog.channel.share'));
    }
  };

  const telegramLink = channel.username ? `https://t.me/${channel.username}` : (channel.inviteLink ?? null);

  const handleOpenTelegram = () => {
    if (telegramLink) {
      window.open(telegramLink, '_blank');
    }
  };

  const hasPricing = channel.pricingRules.length > 0;
  const hasConditions = !!channel.rules;
  const showTabs = hasPricing && hasConditions;

  const pricingConditionsTabs = [
    { value: 'pricing' as const, label: t('catalog.channel.pricingOverview') },
    { value: 'conditions' as const, label: t('catalog.channel.conditions') },
  ];

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
                <span style={{ color: 'var(--color-static-white)', fontSize: 22, fontWeight: 700, lineHeight: 1 }}>
                  {letter}
                </span>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Text type="title2" weight="bold">
                    <span
                      style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}
                    >
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
                <Text type="subheadline1" color="secondary">
                  <span
                    style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                  >
                    {channel.username ? `@${channel.username}` : t('catalog.channel.privateChannel')}
                    <span style={{ color: 'var(--color-foreground-tertiary)' }}>
                      {' \u00b7 '}
                      {formatChannelAge(channel.createdAt, t)}
                    </span>
                  </span>
                </Text>
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

        {/* Owner's note */}
        {channel.rules?.customRules && (
          <motion.div {...slideUp} style={{ padding: '0 16px 8px' }}>
            <div
              style={{
                padding: '14px 16px',
                borderRadius: 12,
                background: 'color-mix(in srgb, var(--color-accent-primary) 5%, var(--color-background-base))',
                border: '1px solid color-mix(in srgb, var(--color-accent-primary) 12%, transparent)',
              }}
            >
              <span
                style={{
                  display: 'block',
                  fontSize: 11,
                  fontWeight: 700,
                  color: 'var(--color-accent-primary)',
                  letterSpacing: '0.04em',
                  textTransform: 'uppercase',
                  marginBottom: 8,
                }}
              >
                {t('catalog.channel.ownerNote')}
              </span>
              <Text type="subheadline1" color="secondary" style={{ whiteSpace: 'pre-wrap' }}>
                {channel.rules.customRules}
              </Text>
            </div>
          </motion.div>
        )}

        {/* Open in Telegram card */}
        {telegramLink && (
          <motion.div {...slideUp} style={{ padding: '0 16px 8px' }}>
            <motion.button
              {...pressScale}
              type="button"
              onClick={handleOpenTelegram}
              style={{
                width: '100%',
                background: 'var(--color-background-base)',
                border: '1px solid var(--color-border-separator)',
                borderRadius: 12,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '14px 16px',
                WebkitTapHighlightColor: 'transparent',
              }}
            >
              <div
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: '50%',
                  background: 'color-mix(in srgb, var(--color-link) 8%, transparent)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                <TelegramIcon style={{ width: 18, height: 18, color: 'var(--color-link)' }} />
              </div>
              <div style={{ flex: 1, minWidth: 0, textAlign: 'left' }}>
                <span style={{ display: 'block', fontSize: 14, fontWeight: 500, color: 'var(--color-link)' }}>
                  {channel.username ? t('catalog.channel.openInTelegram') : t('catalog.channel.joinChannel')}
                </span>
                {channel.username && (
                  <span
                    style={{ display: 'block', fontSize: 12, color: 'var(--color-foreground-tertiary)', marginTop: 2 }}
                  >
                    @{channel.username}
                  </span>
                )}
              </div>
              <ArrowRightIcon
                style={{ width: 16, height: 16, color: 'var(--color-link)', opacity: 0.5, flexShrink: 0 }}
              />
            </motion.button>
          </motion.div>
        )}

        {/* Segment tabs for Pricing / Conditions */}
        {showTabs && (
          <motion.div {...slideUp} style={{ padding: '8px 16px 12px' }}>
            <SegmentControl tabs={pricingConditionsTabs} active={activeTab} onChange={setActiveTab} />
          </motion.div>
        )}

        {showTabs ? (
          <AnimatePresence mode="wait">
            {activeTab === 'pricing' && (
              <motion.div
                key="pricing"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.15 }}
                style={{ padding: '0 16px 16px' }}
              >
                <div style={{ marginBottom: 16 }}>
                  {minPrice != null && (
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, flexWrap: 'wrap' }}>
                      <Text type="title1" weight="bold">
                        <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                          {t('catalog.channel.from', { price: formatTon(minPrice) })}
                        </span>
                      </Text>
                      <Text type="caption1" color="secondary">
                        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(minPrice)}</span>
                      </Text>
                    </div>
                  )}
                  {heroCpm != null && (
                    <Text type="subheadline2" color="secondary" style={{ marginTop: 4 }}>
                      <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                        {'\u2248 '}
                        {formatCpm(heroCpm)} TON {t('catalog.channel.perThousandViews')}
                      </span>
                    </Text>
                  )}
                </div>
                <PricingRulesList rules={channel.pricingRules} channel={channel} t={t} />
              </motion.div>
            )}

            {activeTab === 'conditions' && (
              <motion.div
                key="conditions"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.15 }}
                style={{ padding: '0 16px 16px' }}
              >
                {channel.rules ? (
                  <ChannelRulesSection rules={channel.rules} t={t} />
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
            )}
          </AnimatePresence>
        ) : (
          <>
            {/* Pricing section (no tabs) */}
            {hasPricing && (
              <motion.div {...slideUp} style={{ padding: '8px 16px 16px' }}>
                <div style={{ marginBottom: 16 }}>
                  <Text type="title3" weight="bold" style={{ marginBottom: 6 }}>
                    {t('catalog.channel.pricingOverview')}
                  </Text>
                  {minPrice != null && (
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, flexWrap: 'wrap' }}>
                      <Text type="title1" weight="bold">
                        <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                          {t('catalog.channel.from', { price: formatTon(minPrice) })}
                        </span>
                      </Text>
                      <Text type="caption1" color="secondary">
                        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(minPrice)}</span>
                      </Text>
                    </div>
                  )}
                  {heroCpm != null && (
                    <Text type="subheadline2" color="secondary" style={{ marginTop: 4 }}>
                      <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                        {'\u2248 '}
                        {formatCpm(heroCpm)} TON {t('catalog.channel.perThousandViews')}
                      </span>
                    </Text>
                  )}
                </div>
                <PricingRulesList rules={channel.pricingRules} channel={channel} t={t} />
              </motion.div>
            )}

            {/* Placement conditions (no tabs) */}
            <motion.div {...slideUp} style={{ padding: '0 16px 16px' }}>
              <Text type="title3" weight="bold" style={{ marginBottom: 12 }}>
                {t('catalog.channel.conditions')}
              </Text>
              {channel.rules ? (
                <ChannelRulesSection rules={channel.rules} t={t} />
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
          </>
        )}
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
              <Text type="caption1" color="tertiary">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(minPrice)}</span>
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
  overflow: 'hidden',
};

const postTypeIconContainerStyle: React.CSSProperties = {
  width: 40,
  height: 40,
  borderRadius: 10,
  background: 'var(--color-background-secondary)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  flexShrink: 0,
};

const ruleItemStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'flex-start',
  gap: 8,
};

const overlapBadgeStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 4,
  padding: '2px 8px',
  borderRadius: 6,
  background: 'var(--color-background-secondary)',
  fontSize: 12,
  fontWeight: 600,
  color: 'var(--color-foreground-secondary)',
  fontVariantNumeric: 'tabular-nums',
};

const cpmBadgeStyle: React.CSSProperties = {
  display: 'inline-flex',
  padding: '2px 8px',
  borderRadius: 6,
  background: 'var(--color-background-secondary)',
  fontVariantNumeric: 'tabular-nums',
};

function PricingRulesList({
  rules,
  channel,
  t,
}: {
  rules: PricingRule[];
  channel: ChannelDetail;
  t: (key: string, opts?: Record<string, unknown>) => string;
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {rules.map((rule) => {
        const ruleCpm = channel.avgReach ? computeCpm(rule.priceNano, channel.avgReach) : null;
        const localizedType = t(`catalog.channel.postType.${rule.postType}`, {
          defaultValue: rule.postType,
        });
        const overlap = getOverlapLabel(rule, channel.postFrequencyHours, t);
        return (
          <div key={rule.id} style={pricingCardStyle}>
            <div style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={postTypeIconContainerStyle}>
                <PostTypeIcon
                  postType={rule.postType}
                  style={{ width: 20, height: 20, color: 'var(--color-foreground-secondary)' }}
                />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Text type="body" weight="medium">
                    <span
                      style={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        display: 'block',
                      }}
                    >
                      {localizedType}
                    </span>
                  </Text>
                  {rule.description && (
                    <Popover
                      content={
                        <Text type="caption1" color="secondary" style={{ whiteSpace: 'pre-wrap', maxWidth: 220 }}>
                          {rule.description}
                        </Text>
                      }
                    >
                      <InfoIcon
                        style={{ width: 14, height: 14, color: 'var(--color-foreground-tertiary)', flexShrink: 0 }}
                      />
                    </Popover>
                  )}
                </div>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 6,
                    marginTop: 4,
                    flexWrap: 'wrap',
                  }}
                >
                  {overlap &&
                    (overlap.hasTooltip ? (
                      <Popover
                        content={
                          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <Text type="caption1" color="secondary">
                              {t('catalog.channel.overlapTooltipLine1', { freq: overlap.freq })}
                            </Text>
                            <Text type="caption1" color="secondary">
                              {t('catalog.channel.overlapTooltipLine2', { dur: overlap.dur })}
                            </Text>
                          </div>
                        }
                      >
                        <span style={overlapBadgeStyle}>
                          {overlap.label}
                          <InfoIcon style={{ width: 12, height: 12, color: 'var(--color-foreground-tertiary)' }} />
                        </span>
                      </Popover>
                    ) : (
                      <span style={overlapBadgeStyle}>{overlap.label}</span>
                    ))}
                  {ruleCpm != null && (
                    <span style={cpmBadgeStyle}>
                      <Text type="caption1" color="secondary">
                        {t('catalog.channel.cpmShort', { value: formatCpm(ruleCpm) })}
                      </Text>
                    </span>
                  )}
                </div>
              </div>
              <div style={{ flexShrink: 0, textAlign: 'right' }}>
                <Text type="callout" weight="bold">
                  <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(rule.priceNano)}</span>
                </Text>
                <Text type="caption1" color="tertiary">
                  <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(rule.priceNano)}</span>
                </Text>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
