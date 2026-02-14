import { Button, Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useOnboardingStore } from '@/features/onboarding';
import { NewspaperIcon } from '@/shared/ui/icons';
import { ChannelHeader } from './channel-header';
import { MiniTimeline } from './mini-timeline';
import { MockupContainer } from './mockup-container';
import { MockupTextButton } from './mockup-text-button';
import { TaskHint } from './task-hint';

type DealView = 'timeline' | 'allStates';
type DealState = 'initial' | 'approved';

export function TourSlideDeal() {
  const { t } = useTranslation();
  const [dealView, setDealView] = useState<DealView>('timeline');
  const [dealState, setDealState] = useState<DealState>('initial');
  const [expandedStep, setExpandedStep] = useState<number | null>(1);
  const { completeTourTask } = useOnboardingStore();

  const initialSteps = useMemo(
    () => [
      {
        label: t('onboarding.tour.timeline.macro1'),
        status: 'completed' as const,
        description: t('onboarding.tour.timeline.macro1Desc'),
      },
      {
        label: t('onboarding.tour.timeline.macro2'),
        status: 'active' as const,
        description: t('onboarding.tour.timeline.macro2Desc'),
      },
      {
        label: t('onboarding.tour.timeline.macro3'),
        status: 'pending' as const,
        description: t('onboarding.tour.timeline.macro3Desc'),
      },
    ],
    [t],
  );

  const approvedSteps = useMemo(
    () =>
      initialSteps.map((step, index) => {
        if (index === 0 || index === 1) return { ...step, status: 'completed' as const };
        if (index === 2) return { ...step, status: 'active' as const };
        return step;
      }),
    [initialSteps],
  );

  const negotiationStates = useMemo(
    () => [
      { key: 'draft', text: t('onboarding.tour.allStates.draft'), desc: t('onboarding.tour.allStates.draftDesc') },
      {
        key: 'offerPending',
        text: t('onboarding.tour.allStates.offerPending'),
        desc: t('onboarding.tour.allStates.offerPendingDesc'),
      },
      {
        key: 'negotiating',
        text: t('onboarding.tour.allStates.negotiating'),
        desc: t('onboarding.tour.allStates.negotiatingDesc'),
      },
      {
        key: 'accepted',
        text: t('onboarding.tour.allStates.accepted'),
        desc: t('onboarding.tour.allStates.acceptedDesc'),
      },
    ],
    [t],
  );

  const paymentStates = useMemo(
    () => [
      {
        key: 'awaitingPayment',
        text: t('onboarding.tour.allStates.awaitingPayment'),
        desc: t('onboarding.tour.allStates.awaitingPaymentDesc'),
      },
      {
        key: 'funded',
        text: t('onboarding.tour.allStates.funded'),
        desc: t('onboarding.tour.allStates.fundedDesc'),
      },
      {
        key: 'creativeSubmitted',
        text: t('onboarding.tour.allStates.creativeSubmitted'),
        desc: t('onboarding.tour.allStates.creativeSubmittedDesc'),
      },
      {
        key: 'creativeApproved',
        text: t('onboarding.tour.allStates.creativeApproved'),
        desc: t('onboarding.tour.allStates.creativeApprovedDesc'),
      },
      {
        key: 'scheduled',
        text: t('onboarding.tour.allStates.scheduled'),
        desc: t('onboarding.tour.allStates.scheduledDesc'),
      },
    ],
    [t],
  );

  const publishStates = useMemo(
    () => [
      {
        key: 'published',
        text: t('onboarding.tour.allStates.published'),
        desc: t('onboarding.tour.allStates.publishedDesc'),
      },
      {
        key: 'deliveryVerifying',
        text: t('onboarding.tour.allStates.deliveryVerifying'),
        desc: t('onboarding.tour.allStates.deliveryVerifyingDesc'),
      },
      {
        key: 'completedReleased',
        text: t('onboarding.tour.allStates.completedReleased'),
        desc: t('onboarding.tour.allStates.completedReleasedDesc'),
      },
    ],
    [t],
  );

  const specialStates = useMemo(
    () => [
      {
        key: 'disputed',
        text: t('onboarding.tour.allStates.disputed'),
        desc: t('onboarding.tour.allStates.disputedDesc'),
      },
      {
        key: 'cancelled',
        text: t('onboarding.tour.allStates.cancelled'),
        desc: t('onboarding.tour.allStates.cancelledDesc'),
      },
      {
        key: 'refunded',
        text: t('onboarding.tour.allStates.refunded'),
        desc: t('onboarding.tour.allStates.refundedDesc'),
      },
      {
        key: 'partiallyRefunded',
        text: t('onboarding.tour.allStates.partiallyRefunded'),
        desc: t('onboarding.tour.allStates.partiallyRefundedDesc'),
      },
      {
        key: 'expired',
        text: t('onboarding.tour.allStates.expired'),
        desc: t('onboarding.tour.allStates.expiredDesc'),
      },
    ],
    [t],
  );

  function handleApprove() {
    setDealState('approved');
    setExpandedStep(2);
    completeTourTask(1);
  }

  function handleStepClick(index: number) {
    setExpandedStep((prev) => (prev === index ? null : index));
  }

  useEffect(() => {
    if (dealState === 'approved') {
      const timer = setTimeout(() => {
        setDealState('initial');
        setExpandedStep(1);
      }, 2500);
      return () => clearTimeout(timer);
    }
  }, [dealState]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <Text type="title2" weight="bold" align="center">
        {t('onboarding.tour.slide2.title')}
      </Text>
      <Text type="caption1" color="secondary" align="center">
        {t('onboarding.tour.slide2.hint')}
      </Text>

      <MockupContainer>
        <AnimatePresence mode="wait">
          {dealView === 'timeline' ? (
            <motion.div key="timeline" exit={{ opacity: 0 }} transition={{ duration: 0.2 }}>
              <ChannelHeader
                icon={<NewspaperIcon style={{ width: 18, height: 18, color: 'var(--color-foreground-secondary)' }} />}
                name={t('onboarding.tour.mockup.channelName1')}
                detail="5.00 TON"
              />

              <AnimatePresence mode="wait">
                <motion.div
                  key={dealState}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.2 }}
                >
                  <MiniTimeline
                    steps={dealState === 'initial' ? initialSteps : approvedSteps}
                    onActiveClick={dealState === 'initial' ? handleApprove : undefined}
                    expandedIndex={expandedStep}
                    onStepClick={handleStepClick}
                  />
                </motion.div>
              </AnimatePresence>

              <div style={{ textAlign: 'center', padding: '4px 0 8px' }}>
                <Text type="caption1" color="tertiary">
                  {t('onboarding.tour.timeline.simplifiedNote')}
                </Text>
              </div>

              <div style={{ marginTop: '4px' }}>
                <Button
                  text={t('onboarding.tour.mockup.approve')}
                  type="primary"
                  disabled={dealState === 'approved'}
                  onClick={dealState === 'initial' ? handleApprove : undefined}
                />
              </div>

              <AnimatePresence>
                {dealState === 'approved' && (
                  <motion.div
                    role="status"
                    aria-live="polite"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.2 }}
                    style={{ textAlign: 'center', marginTop: '8px' }}
                  >
                    <Text type="caption1" color="accent">
                      {t('onboarding.tour.slide2.taskDone')}
                    </Text>
                  </motion.div>
                )}
              </AnimatePresence>

              <MockupTextButton
                text={t('onboarding.tour.allStates.link')}
                color="accent"
                onClick={() => setDealView('allStates')}
              />
            </motion.div>
          ) : (
            <motion.div
              key="allStates"
              initial={{ opacity: 0, scale: 1.02 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
            >
              <Group header={t('onboarding.tour.allStates.groupNegotiation')}>
                {negotiationStates.map((s) => (
                  <GroupItem key={s.key} text={s.text} description={s.desc} />
                ))}
              </Group>
              <Group header={t('onboarding.tour.allStates.groupPayment')}>
                {paymentStates.map((s) => (
                  <GroupItem key={s.key} text={s.text} description={s.desc} />
                ))}
              </Group>
              <Group header={t('onboarding.tour.allStates.groupPublish')}>
                {publishStates.map((s) => (
                  <GroupItem key={s.key} text={s.text} description={s.desc} />
                ))}
              </Group>
              <Group header={t('onboarding.tour.allStates.groupSpecial')}>
                {specialStates.map((s) => (
                  <GroupItem key={s.key} text={s.text} description={s.desc} />
                ))}
              </Group>

              <MockupTextButton text={t('onboarding.tour.allStates.back')} onClick={() => setDealView('timeline')} />
            </motion.div>
          )}
        </AnimatePresence>
      </MockupContainer>

      <AnimatePresence>
        {dealView === 'timeline' && dealState === 'initial' && (
          <motion.div exit={{ opacity: 0 }}>
            <TaskHint text={t('onboarding.tour.slide2.taskHint')} />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
