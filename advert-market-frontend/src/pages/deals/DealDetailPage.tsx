import { Sheet, Spinner, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { DealActions } from '@/features/deals/components/DealActions';
import { DealInfoCard } from '@/features/deals/components/DealInfoCard';
import { DealStatusBadge } from '@/features/deals/components/DealStatusBadge';
import { DealTimeline } from '@/features/deals/components/DealTimeline';
import { NegotiateSheetContent, setNegotiateSheetProps } from '@/features/deals/components/NegotiateSheet';
import { useDealDetail } from '@/features/deals/hooks/useDealDetail';
import { useDealTransition } from '@/features/deals/hooks/useDealTransition';
import { getDealActions } from '@/features/deals/lib/deal-actions';
import { buildTimelineSteps, getStatusConfig, statusBgVar } from '@/features/deals/lib/deal-status';
import type { DealActionType } from '@/features/deals/lib/deal-actions';
import { BackButtonHandler, EmptyState } from '@/shared/ui';
import { fadeIn } from '@/shared/ui/animations';

const sheetMap = {
  negotiate: NegotiateSheetContent,
};

export default function DealDetailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { dealId } = useParams<{ dealId: string }>();
  const [sheetOpen, setSheetOpen] = useState(false);

  const { deal, timeline, isLoading, isError } = useDealDetail(dealId!);
  const { transition, negotiate, isPending } = useDealTransition(dealId!);

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
      if (deal) {
        setNegotiateSheetProps({
          currentPriceNano: deal.priceNano,
          onSubmit: (priceNano, message) => {
            negotiate({ priceNano, message });
            setSheetOpen(false);
          },
          isPending,
        });
        setSheetOpen(true);
      }
      return;
    }
    transition({ action: type });
  };

  if (isLoading) {
    return (
      <>
        <BackButtonHandler />
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
          <Spinner size="32px" color="accent" />
        </div>
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
        {/* Channel header */}
        <div
          style={{
            padding: '16px 16px 14px',
            background: 'var(--color-background-base)',
            borderBottom: '1px solid var(--color-border-separator)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: '50%',
                background: `hsl(${hue}, 55%, 55%)`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <span style={{ color: 'var(--color-static-white)', fontSize: 20, fontWeight: 600, lineHeight: 1 }}>
                {deal.channelTitle.charAt(0).toUpperCase()}
              </span>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Text type="title2" weight="bold">
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                    {deal.channelTitle}
                  </span>
                </Text>
              </div>
              <Text type="subheadline2" color="secondary">
                {t(`catalog.channel.postType.${deal.postType}`)}
              </Text>
            </div>
          </div>

          {/* Status banner */}
          <div
            style={{
              marginTop: 10,
              padding: '10px 14px',
              borderRadius: 10,
              background: statusBgVar(statusConfig.color),
            }}
          >
            <DealStatusBadge status={deal.status} />
            <Text type="caption1" color="secondary" style={{ marginTop: 6, display: 'block' }}>
              {t(statusDescKey)}
            </Text>
          </div>
        </div>

        {/* Info card */}
        <div style={{ padding: '16px 0', marginBottom: 0 }}>
          <DealInfoCard deal={deal} />
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
      <Sheet
        sheets={sheetMap}
        activeSheet="negotiate"
        opened={sheetOpen}
        onClose={() => setSheetOpen(false)}
      />
    </>
  );
}
