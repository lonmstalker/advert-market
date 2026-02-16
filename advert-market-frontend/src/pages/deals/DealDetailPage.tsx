import { Sheet, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { DealActions } from '@/features/deals/components/DealActions';
import { DealTimeline } from '@/features/deals/components/DealTimeline';
import { NegotiateProvider } from '@/features/deals/components/NegotiateContext';
import { NegotiateSheetContent } from '@/features/deals/components/NegotiateSheet';
import { PaymentProvider } from '@/features/deals/components/PaymentContext';
import { PaymentSheetContent } from '@/features/deals/components/PaymentSheet';
import { useDealDetail } from '@/features/deals/hooks/useDealDetail';
import { useDealTransition } from '@/features/deals/hooks/useDealTransition';
import type { DealActionType } from '@/features/deals/lib/deal-actions';
import { getDealActions } from '@/features/deals/lib/deal-actions';
import { buildTimelineSteps, getStatusConfig } from '@/features/deals/lib/deal-status';
import { useCountdown } from '@/shared/hooks/use-countdown';
import { useToast } from '@/shared/hooks/use-toast';
import { loadPendingIntent } from '@/shared/ton';
import { AppPageShell, BackButtonHandler, EmptyState, PageLoader } from '@/shared/ui';
import { fadeIn } from '@/shared/ui/animations';
import { DocumentIcon, SadFaceIcon } from '@/shared/ui/icons';
import { DealHeroSection } from './components/DealHeroSection';

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
  payment: PaymentSheetContent,
};

type DealSheet = 'negotiate' | 'payment';

export default function DealDetailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { dealId } = useParams<{ dealId: string }>();
  const [activeSheet, setActiveSheet] = useState<DealSheet>('negotiate');
  const [sheetOpen, setSheetOpen] = useState(false);
  const { showError } = useToast();

  useEffect(() => {
    if (!dealId) navigate('/deals', { replace: true });
  }, [dealId, navigate]);

  const safeDealId = dealId ?? '';
  const { deal, timeline, isLoading, isError } = useDealDetail(safeDealId);
  const { transition, negotiate, isPending } = useDealTransition(safeDealId);
  const countdown = useCountdown(deal?.deadlineAt ?? null, t);

  const actions = useMemo(() => {
    if (!deal) return [];
    return getDealActions(deal.status, deal.role);
  }, [deal]);

  const timelineSteps = useMemo(() => {
    if (!deal || !timeline) return [];
    return buildTimelineSteps(timeline.events, deal.status, t);
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
    if (type === 'counter_offer' || type === 'reply') {
      setActiveSheet('negotiate');
      setSheetOpen(true);
      return;
    }
    if (type === 'pay') {
      setActiveSheet('payment');
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
          icon={<SadFaceIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
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

  return (
    <>
      <BackButtonHandler />
      <AppPageShell withTabsPadding={false} testId="deal-detail-page-shell">
        <motion.div {...fadeIn} className="flex flex-col min-h-[calc(100vh-40px)]">
          <DealHeroSection deal={deal} statusConfig={statusConfig} isTerminal={isTerminal} countdown={countdown} />

          {deal.status.includes('CREATIVE') && (
            <div className="pb-3">
              <div className="am-surface-row flex flex-col items-center gap-2 py-5 px-4 text-center">
                <DocumentIcon size={28} className="text-fg-tertiary" />
                <Text type="caption1" color="secondary">
                  {t('deals.detail.creativePlaceholder')}
                </Text>
              </div>
            </div>
          )}

          <div
            className={actions.length > 0 ? 'pb-[calc(var(--am-fixed-bottom-bar-base,92px)+var(--am-safe-area-bottom))]' : 'pb-4'}
          >
            {timelineSteps.length > 0 && <DealTimeline steps={timelineSteps} />}
          </div>
        </motion.div>
      </AppPageShell>

      {actions.length > 0 && <DealActions actions={actions} onAction={handleAction} isPending={isPending} />}

      <PaymentProvider dealId={safeDealId} onClose={() => setSheetOpen(false)}>
        <NegotiateProvider
          currentPriceNano={deal?.priceNano ?? 0}
          onSubmit={(priceNano, message) => {
            negotiate({ priceNano, message });
            setSheetOpen(false);
          }}
          isPending={isPending}
        >
          <Sheet sheets={sheetMap} activeSheet={activeSheet} opened={sheetOpen} onClose={() => setSheetOpen(false)} />
        </NegotiateProvider>
      </PaymentProvider>
    </>
  );
}
