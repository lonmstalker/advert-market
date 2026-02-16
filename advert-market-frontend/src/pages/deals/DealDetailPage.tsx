import { useQuery } from '@tanstack/react-query';
import { Sheet, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { fetchChannelDetail } from '@/features/channels';
import { DealActions } from '@/features/deals/components/DealActions';
import { DealTimeline } from '@/features/deals/components/DealTimeline';
import { NegotiateProvider } from '@/features/deals/components/NegotiateContext';
import { NegotiateSheetContent } from '@/features/deals/components/NegotiateSheet';
import { PaymentProvider } from '@/features/deals/components/PaymentContext';
import { PaymentSheetContent } from '@/features/deals/components/PaymentSheet';
import { useDealDetail } from '@/features/deals/hooks/useDealDetail';
import { useDealTransition } from '@/features/deals/hooks/useDealTransition';
import type { DealAction, DealActionType } from '@/features/deals/lib/deal-actions';
import { getDealActions } from '@/features/deals/lib/deal-actions';
import { mapDealDetailDtoToViewModel } from '@/features/deals/lib/deal-mapper';
import { buildTimelineSteps, getStatusConfig } from '@/features/deals/lib/deal-status';
import type { DealChannelMetadata, DealStatus } from '@/features/deals/types/deal';
import { fetchProfile } from '@/shared/api/auth';
import { channelKeys, profileKeys } from '@/shared/api/query-keys';
import { useCountdown } from '@/shared/hooks/use-countdown';
import { useToast } from '@/shared/hooks/use-toast';
import { loadPendingIntent } from '@/shared/ton';
import { AppPageShell, BackButtonHandler, EmptyState, PageLoader } from '@/shared/ui';
import { fadeIn } from '@/shared/ui/animations';
import { DocumentIcon, SadFaceIcon } from '@/shared/ui/icons';
import { DealHeroSection } from './components/DealHeroSection';

const TERMINAL_STATUSES = new Set<DealStatus>([
  'COMPLETED_RELEASED',
  'CANCELLED',
  'EXPIRED',
  'REFUNDED',
  'PARTIALLY_REFUNDED',
  'DISPUTED',
]);

const CREATIVE_FLOW_ENABLED = false;
const CREATIVE_BLOCKED_STATUSES = new Set<DealStatus>(['FUNDED', 'CREATIVE_SUBMITTED', 'CREATIVE_APPROVED']);

const sheetMap = {
  negotiate: NegotiateSheetContent,
  payment: PaymentSheetContent,
};

type DealSheet = 'negotiate' | 'payment';

function mapChannelMetadata(detail: Awaited<ReturnType<typeof fetchChannelDetail>>): DealChannelMetadata {
  const firstRule = detail.pricingRules[0];
  return {
    title: detail.title,
    username: detail.username ?? null,
    postType: firstRule?.postTypes?.[0] ?? null,
    durationHours: null,
    postFrequencyHours: detail.postFrequencyHours ?? null,
  };
}

export default function DealDetailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { dealId } = useParams<{ dealId: string }>();
  const [activeSheet, setActiveSheet] = useState<DealSheet>('negotiate');
  const [sheetOpen, setSheetOpen] = useState(false);
  const [pendingReasonAction, setPendingReasonAction] = useState<DealAction | null>(null);
  const { showError } = useToast();

  useEffect(() => {
    if (!dealId) navigate('/deals', { replace: true });
  }, [dealId, navigate]);

  const safeDealId = dealId ?? '';
  const { deal: dealDto, timeline, isLoading: isDealLoading, isError } = useDealDetail(safeDealId);
  const { transition, isPending } = useDealTransition(safeDealId);

  const { data: profile, isLoading: isProfileLoading } = useQuery({
    queryKey: profileKeys.me,
    queryFn: fetchProfile,
    enabled: Boolean(dealDto),
  });

  const { data: channelDetail } = useQuery({
    queryKey: channelKeys.detail(dealDto?.channelId ?? 0),
    queryFn: () => fetchChannelDetail(dealDto?.channelId ?? 0),
    enabled: Boolean(dealDto?.channelId),
  });

  const channelMeta = useMemo(() => (channelDetail ? mapChannelMetadata(channelDetail) : null), [channelDetail]);

  const deal = useMemo(() => {
    if (!dealDto) return null;
    return mapDealDetailDtoToViewModel(dealDto, {
      profileId: profile?.id,
      channel: channelMeta,
    });
  }, [channelMeta, dealDto, profile?.id]);

  const countdown = useCountdown(deal?.deadlineAt ?? null, t);

  const actions = useMemo(() => {
    if (!deal) return [];
    return getDealActions(deal.status, deal.role, {
      creativeFlowEnabled: CREATIVE_FLOW_ENABLED,
    });
  }, [deal]);

  const timelineSteps = useMemo(() => {
    if (!deal || !timeline) return [];
    return buildTimelineSteps(deal.timeline, deal.status, t);
  }, [deal, timeline, t]);

  useEffect(() => {
    if (!dealId) return;
    const raw = sessionStorage.getItem('ton_pending_intent');
    if (!raw) return;

    const intent = loadPendingIntent();
    if (!intent) {
      showError(t('wallet.error.pollingTimeout'));
      return;
    }
    if (intent.dealId !== dealId) return;

    setActiveSheet('payment');
    setSheetOpen(true);
  }, [dealId, showError, t]);

  const handleAction = (type: DealActionType) => {
    const action = actions.find((candidate) => candidate.type === type);
    if (!action) return;

    if (action.type === 'pay') {
      if (deal?.status !== 'AWAITING_PAYMENT') return;
      setActiveSheet('payment');
      setSheetOpen(true);
      return;
    }

    if (action.requiresReason) {
      setPendingReasonAction(action);
      setActiveSheet('negotiate');
      setSheetOpen(true);
      return;
    }

    if (!action.targetStatus) return;
    transition({ targetStatus: action.targetStatus });
  };

  const isLoading = isDealLoading || isProfileLoading;

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
          icon={<SadFaceIcon className="w-7 h-7 text-fg-tertiary" />}
          title={t('errors.notFound')}
          description={t('deals.empty.description')}
          actionLabel={t('common.back')}
          onAction={() => navigate(-1)}
        />
      </>
    );
  }

  const statusConfig = getStatusConfig(deal.status);
  const isTerminal = TERMINAL_STATUSES.has(deal.status);
  const isCreativeBlocked = !CREATIVE_FLOW_ENABLED && CREATIVE_BLOCKED_STATUSES.has(deal.status);

  return (
    <>
      <BackButtonHandler />
      <AppPageShell withTabsPadding={false} testId="deal-detail-page-shell">
        <motion.div {...fadeIn} className="flex flex-col min-h-[calc(100vh-40px)]">
          <DealHeroSection deal={deal} statusConfig={statusConfig} isTerminal={isTerminal} countdown={countdown} />

          {isCreativeBlocked && (
            <div className="pb-4">
              <div className="am-surface-row flex flex-col items-center gap-3 py-6 px-4 text-center">
                <DocumentIcon size={28} className="text-fg-tertiary" />
                <Text type="caption1" color="secondary">
                  {t('deals.detail.creativeBlocked')}
                </Text>
              </div>
            </div>
          )}

          <div
            className={
              actions.length > 0 ? 'pb-[calc(var(--am-fixed-bottom-bar-base,92px)+var(--am-safe-area-bottom))]' : 'pb-6'
            }
          >
            {timelineSteps.length > 0 && <DealTimeline steps={timelineSteps} />}
          </div>
        </motion.div>
      </AppPageShell>

      {actions.length > 0 && <DealActions actions={actions} onAction={handleAction} isPending={isPending} />}

      <PaymentProvider dealId={safeDealId} onClose={() => setSheetOpen(false)}>
        <NegotiateProvider
          actionLabelKey={pendingReasonAction?.i18nKey ?? 'deals.actions.counterOffer'}
          reasonRequired={pendingReasonAction?.requiresReason ?? false}
          onSubmit={(reason) => {
            if (!pendingReasonAction?.targetStatus) return;
            transition({
              targetStatus: pendingReasonAction.targetStatus,
              reason,
            });
            setSheetOpen(false);
            setPendingReasonAction(null);
          }}
          isPending={isPending}
        >
          <Sheet
            sheets={sheetMap}
            activeSheet={activeSheet}
            opened={sheetOpen}
            onClose={() => {
              setSheetOpen(false);
              setPendingReasonAction(null);
            }}
          />
        </NegotiateProvider>
      </PaymentProvider>
    </>
  );
}
