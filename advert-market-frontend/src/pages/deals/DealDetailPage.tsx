import { Sheet, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { DealActions } from '@/features/deals/components/DealActions';
import { DealStatusBadge } from '@/features/deals/components/DealStatusBadge';
import { DealTimeline } from '@/features/deals/components/DealTimeline';
import { NegotiateProvider } from '@/features/deals/components/NegotiateContext';
import { NegotiateSheetContent } from '@/features/deals/components/NegotiateSheet';
import { useDealDetail } from '@/features/deals/hooks/useDealDetail';
import { useDealTransition } from '@/features/deals/hooks/useDealTransition';
import type { DealActionType } from '@/features/deals/lib/deal-actions';
import { getDealActions } from '@/features/deals/lib/deal-actions';
import { buildTimelineSteps, getStatusConfig, statusBgVar } from '@/features/deals/lib/deal-status';
import { useCountdown } from '@/shared/hooks/use-countdown';
import { formatDate } from '@/shared/lib/date-format';
import { formatFiat } from '@/shared/lib/fiat-format';
import { buildOverlapLabel } from '@/shared/lib/overlap-label';
import { formatTon } from '@/shared/lib/ton-format';
import { BackButtonHandler, EmptyState, PageLoader, Popover } from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';
import { InfoIcon, TelegramIcon, TonDiamondIcon } from '@/shared/ui/icons';

const TERMINAL_STATUSES = new Set([
  'COMPLETED_RELEASED',
  'CANCELLED',
  'EXPIRED',
  'REFUNDED',
  'PARTIALLY_REFUNDED',
  'DISPUTED',
]);

const sheetMap = {
  negotiate: NegotiateSheetContent,
};

export default function DealDetailPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { dealId } = useParams<{ dealId: string }>();
  const [sheetOpen, setSheetOpen] = useState(false);

  const { deal, timeline, isLoading, isError } = useDealDetail(dealId as string);
  const { transition, negotiate, isPending } = useDealTransition(dealId as string);
  const countdown = useCountdown(deal?.deadlineAt ?? null, t);

  const actions = useMemo(() => {
    if (!deal) return [];
    return getDealActions(deal.status, deal.role);
  }, [deal]);

  const timelineSteps = useMemo(() => {
    if (!deal || !timeline) return [];
    return buildTimelineSteps(timeline.events, deal.status, t);
  }, [deal, timeline, t]);

  const handleAction = (type: DealActionType) => {
    if (type === 'counter_offer' || type === 'reply') {
      setSheetOpen(true);
      return;
    }
    transition({ action: type });
  };

  if (isLoading) {
    return (
      <>
        <BackButtonHandler />
        <PageLoader />
      </>
    );
  }

  if (isError || !deal) {
    return (
      <>
        <BackButtonHandler />
        <EmptyState
          emoji="😔"
          title={t('errors.notFound')}
          description={t('deals.empty.description')}
          actionLabel={t('common.back')}
          onAction={() => navigate(-1)}
        />
      </>
    );
  }

  const hue = (deal.channelTitle.charCodeAt(0) * 37 + (deal.channelTitle.charCodeAt(1) || 0) * 53) % 360;
  const statusConfig = getStatusConfig(deal.status);
  const statusDescKey = `${statusConfig.i18nKey}Desc`;
  const isTerminal = TERMINAL_STATUSES.has(deal.status);

  const telegramLink = deal.channelUsername ? `https://t.me/${deal.channelUsername}` : null;

  const overlap = buildOverlapLabel(deal.postFrequencyHours, deal.durationHours, t);
  const overlapLabel = overlap?.label ?? null;
  const hasOverlapTooltip = overlap?.hasTooltip ?? false;
  const freq = overlap?.freq;
  const dur = overlap?.dur;

  return (
    <>
      <BackButtonHandler />
      <motion.div
        {...fadeIn}
        style={{
          display: 'flex',
          flexDirection: 'column',
          minHeight: 'calc(100vh - 40px)',
        }}
      >
        {/* Hero section */}
        <div style={{ position: 'relative', overflow: 'hidden' }}>
          {/* Gradient backdrop */}
          <div
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              height: 160,
              background: `linear-gradient(180deg, ${statusBgVar(statusConfig.color)} 0%, transparent 100%)`,
              pointerEvents: 'none',
            }}
          />

          {/* Decorative diamonds */}
          <TonDiamondIcon
            style={{
              position: 'absolute',
              top: 18,
              right: 32,
              width: 10,
              height: 10,
              opacity: 0.15,
              color: 'var(--color-foreground-primary)',
              pointerEvents: 'none',
            }}
          />
          <TonDiamondIcon
            style={{
              position: 'absolute',
              top: 44,
              right: 72,
              width: 14,
              height: 14,
              opacity: 0.1,
              color: 'var(--color-foreground-primary)',
              pointerEvents: 'none',
            }}
          />
          <TonDiamondIcon
            style={{
              position: 'absolute',
              top: 28,
              left: 24,
              width: 8,
              height: 8,
              opacity: 0.18,
              color: 'var(--color-foreground-primary)',
              pointerEvents: 'none',
            }}
          />

          <div style={{ position: 'relative', padding: '20px 16px 16px' }}>
            {/* Avatar + channel info */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
              <motion.div
                {...(telegramLink ? pressScale : {})}
                onClick={telegramLink ? () => window.open(telegramLink, '_blank') : undefined}
                style={{
                  position: 'relative',
                  width: 48,
                  height: 48,
                  flexShrink: 0,
                  cursor: telegramLink ? 'pointer' : 'default',
                }}
              >
                <div
                  style={{
                    width: 48,
                    height: 48,
                    borderRadius: '50%',
                    background: `hsl(${hue}, 55%, 55%)`,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <span style={{ color: 'var(--color-static-white)', fontSize: 20, fontWeight: 600, lineHeight: 1 }}>
                    {deal.channelTitle.charAt(0).toUpperCase()}
                  </span>
                </div>
                {telegramLink && (
                  <div
                    style={{
                      position: 'absolute',
                      bottom: -2,
                      right: -2,
                      width: 18,
                      height: 18,
                      borderRadius: '50%',
                      background: 'var(--color-link)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      border: '2px solid var(--color-background-base)',
                    }}
                  >
                    <TelegramIcon style={{ width: 10, height: 10, color: 'var(--color-static-white)' }} />
                  </div>
                )}
              </motion.div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <Text type="title2" weight="bold">
                  <span
                    style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}
                  >
                    {deal.channelTitle}
                  </span>
                </Text>
                <Text type="subheadline2" color="secondary">
                  {deal.channelUsername ? `@${deal.channelUsername}` : ''}
                  {deal.channelUsername ? ' \u00b7 ' : ''}
                  {t(`catalog.channel.postType.${deal.postType}`)}
                </Text>
              </div>
            </div>

            {/* Price */}
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1, duration: 0.3 }}
              style={{ textAlign: 'center', marginBottom: 14 }}
            >
              <Text type="title1" weight="bold">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(deal.priceNano)}</span>
              </Text>
              <Text type="caption1" color="secondary" style={{ display: 'block', marginTop: 2 }}>
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(deal.priceNano)}</span>
              </Text>
            </motion.div>

            {/* Chips row */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'center',
                flexWrap: 'wrap',
                gap: 8,
                marginBottom: 14,
              }}
            >
              {overlapLabel &&
                (hasOverlapTooltip ? (
                  <Popover
                    content={
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                        <Text type="caption1" color="secondary">
                          {t('catalog.channel.overlapTooltipLine1', { freq })}
                        </Text>
                        <Text type="caption1" color="secondary">
                          {t('catalog.channel.overlapTooltipLine2', { dur })}
                        </Text>
                      </div>
                    }
                  >
                    <span style={heroChipStyle}>
                      {overlapLabel}
                      <InfoIcon style={{ width: 12, height: 12, color: 'var(--color-foreground-tertiary)' }} />
                    </span>
                  </Popover>
                ) : (
                  <span style={heroChipStyle}>{overlapLabel}</span>
                ))}
              <span style={heroChipStyle}>{formatDate(deal.createdAt, i18n.language)}</span>
              {countdown && (
                <span style={heroChipStyle}>
                  {'\u23f1'} {countdown}
                </span>
              )}
            </div>

            {/* Deal message */}
            {deal.message && (
              <div
                style={{
                  background: 'var(--color-background-secondary)',
                  borderRadius: 10,
                  padding: '10px 14px',
                  marginBottom: 14,
                }}
              >
                <Text
                  type="caption1"
                  color="secondary"
                  style={{ whiteSpace: 'pre-wrap', fontStyle: 'italic', textAlign: 'center' }}
                >
                  &ldquo;{deal.message}&rdquo;
                </Text>
              </div>
            )}

            {/* Status pill */}
            <div
              style={{
                padding: '10px 14px',
                borderRadius: 10,
                background: statusBgVar(statusConfig.color),
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              {!isTerminal && <PulsingDot color={statusConfig.color} />}
              <div>
                <DealStatusBadge status={deal.status} />
                <Text type="caption1" color="secondary" style={{ marginTop: 4, display: 'block' }}>
                  {t(statusDescKey)}
                </Text>
              </div>
            </div>
          </div>
        </div>

        {/* Timeline */}
        <div style={{ paddingBottom: actions.length > 0 ? 80 : 16 }}>
          {timelineSteps.length > 0 && <DealTimeline steps={timelineSteps} />}
        </div>
      </motion.div>

      {/* Actions (sticky bottom) */}
      {actions.length > 0 && (
        <div style={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: 10 }}>
          <DealActions actions={actions} onAction={handleAction} isPending={isPending} />
        </div>
      )}

      {/* Negotiate Sheet */}
      <NegotiateProvider
        currentPriceNano={deal?.priceNano ?? 0}
        onSubmit={(priceNano, message) => {
          negotiate({ priceNano, message });
          setSheetOpen(false);
        }}
        isPending={isPending}
      >
        <Sheet sheets={sheetMap} activeSheet="negotiate" opened={sheetOpen} onClose={() => setSheetOpen(false)} />
      </NegotiateProvider>
    </>
  );
}

const heroChipStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 4,
  padding: '4px 10px',
  borderRadius: 8,
  background: 'var(--color-background-secondary)',
  border: '1px solid var(--color-border-separator)',
  fontSize: 13,
  fontWeight: 500,
  color: 'var(--color-foreground-primary)',
  fontVariantNumeric: 'tabular-nums',
};

const statusColorVars: Record<string, string> = {
  accent: 'var(--color-accent-primary)',
  warning: 'var(--color-warning)',
  success: 'var(--color-success)',
  destructive: 'var(--color-destructive)',
};

function PulsingDot({ color }: { color: string }) {
  const colorVar = statusColorVars[color] ?? 'var(--color-foreground-secondary)';
  return (
    <motion.span
      animate={{ opacity: [1, 0.4, 1] }}
      transition={{ duration: 2, repeat: Number.POSITIVE_INFINITY, ease: 'easeInOut' }}
      style={{
        width: 8,
        height: 8,
        borderRadius: '50%',
        background: colorVar,
        flexShrink: 0,
        display: 'block',
      }}
    />
  );
}
