import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, DialogModal, Text } from '@telegram-tools/ui-kit';
import { easeOut } from 'motion';
import { AnimatePresence, motion } from 'motion/react';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { completeOnboarding } from '@/features/auth';
import { OnboardingShell } from '@/features/onboarding/components/onboarding-shell';
import { TourSlideCatalog } from '@/features/onboarding/components/tour-slide-catalog';
import { TourSlideDeal } from '@/features/onboarding/components/tour-slide-deal';
import { TourSlideWallet } from '@/features/onboarding/components/tour-slide-wallet';
import { resolveOnboardingRoute, useOnboardingStore } from '@/features/onboarding/store/onboarding-store';
import { profileKeys } from '@/shared/api';
import { trackOnboardingEvent } from '@/shared/lib/onboarding-analytics';
import { pressScale, Tappable } from '@/shared/ui';

const SLIDE_COUNT = 3;

const slideVariants = {
  enter: { opacity: 0, x: 40 },
  center: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: -40 },
};

const slideComponents = [TourSlideCatalog, TourSlideDeal, TourSlideWallet];
const slideToStepMap = ['tour-1', 'tour-2', 'tour-3'] as const;

export default function OnboardingTourPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { interests, activeSlide, setActiveSlide, getTaskState, getPrimaryRole, reset } = useOnboardingStore();
  const [showError, setShowError] = useState(false);
  const [showSkipConfirm, setShowSkipConfirm] = useState(false);
  const isAnimating = useRef(false);
  const hasCompleted = useRef(false);
  const primaryRole = getPrimaryRole();

  useEffect(() => {
    if (interests.size === 0 && !hasCompleted.current) {
      navigate('/onboarding/interest', { replace: true });
    }
  }, [interests.size, navigate]);

  useEffect(() => {
    const step = slideToStepMap[activeSlide] ?? 'tour-1';
    trackOnboardingEvent('onboarding_view', { step });
  }, [activeSlide]);

  function completeAndNavigate() {
    setShowError(false);
    mutation.mutate();
  }

  const mutation = useMutation({
    mutationFn: () => completeOnboarding([...interests]),
    onSuccess: (updatedProfile) => {
      hasCompleted.current = true;
      queryClient.setQueryData(profileKeys.me, updatedProfile);
      trackOnboardingEvent('onboarding_complete', { role: primaryRole, variant: 'direct_replace' });
      navigate(resolveOnboardingRoute(primaryRole), { replace: true });
      reset();
    },
    onError: () => {
      setShowError(true);
    },
  });

  const isLastSlide = activeSlide === SLIDE_COUNT - 1;
  const taskState = getTaskState(activeSlide);
  const taskDone = taskState === 'completed';
  const finishText =
    primaryRole === 'owner'
      ? t('onboarding.tour.finishOwner')
      : primaryRole === 'both'
        ? t('onboarding.tour.finishBoth')
        : t('onboarding.tour.finishAdvertiser');

  function handleNext() {
    if (isAnimating.current || mutation.isPending) return;

    const step = slideToStepMap[activeSlide] ?? 'tour-1';
    trackOnboardingEvent('onboarding_primary_click', { step });

    if (isLastSlide) {
      completeAndNavigate();
    } else {
      isAnimating.current = true;
      setActiveSlide(activeSlide + 1);
    }
  }

  function handleAnimationComplete() {
    isAnimating.current = false;
  }

  if (interests.size === 0) return null;

  const SlideComponent = slideComponents[activeSlide];
  if (!SlideComponent) return null;

  return (
    <>
      <OnboardingShell
        testId="onboarding-tour-step"
        topAction={
          <Tappable
            onClick={() => setShowSkipConfirm(true)}
            style={{
              border: 'none',
              background: 'transparent',
              color: 'var(--color-foreground-secondary)',
              minHeight: 36,
              minWidth: 44,
              padding: '8px 0',
              display: 'flex',
              alignItems: 'center',
            }}
            aria-label={t('onboarding.tour.skip')}
          >
            <Text type="subheadline2" color="secondary">
              {t('onboarding.tour.skip')}
            </Text>
          </Tappable>
        }
        contentStyle={{
          justifyContent: 'center',
          position: 'relative',
          overflow: 'hidden',
          paddingTop: 8,
        }}
        footer={
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <motion.div {...pressScale}>
              <Button
                text={isLastSlide ? finishText : t('onboarding.tour.next')}
                type="primary"
                loading={mutation.isPending}
                onClick={handleNext}
              />
            </motion.div>

            {!isLastSlide && (
              <output aria-live="polite" style={{ textAlign: 'center', display: 'block' }}>
                <Text type="caption1" color={taskDone ? 'accent' : 'secondary'}>
                  {taskDone ? t('onboarding.tour.taskStatus.completed') : t('onboarding.tour.taskStatus.recommended')}
                </Text>
              </output>
            )}

            {showError && (
              <div role="alert" style={{ textAlign: 'center' }}>
                <Text type="caption1" color="danger">
                  {t('onboarding.tour.error')}
                </Text>
              </div>
            )}
          </div>
        }
      >
        <AnimatePresence mode="wait">
          <motion.div
            key={activeSlide}
            variants={slideVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ duration: 0.25, ease: easeOut }}
            onAnimationComplete={handleAnimationComplete}
          >
            <SlideComponent primaryRole={primaryRole} />
          </motion.div>
        </AnimatePresence>

        <div
          role="tablist"
          aria-label={t('onboarding.tour.next')}
          style={{
            display: 'flex',
            justifyContent: 'center',
            gap: '6px',
            paddingTop: 12,
            paddingBottom: 4,
          }}
        >
          {(['catalog', 'deal', 'wallet'] as const).map((key, i) => {
            const isActive = i === activeSlide;
            return (
              <motion.div
                key={key}
                role="tab"
                aria-selected={isActive}
                aria-label={`${i + 1} / ${SLIDE_COUNT}`}
                animate={{ width: isActive ? 24 : 8 }}
                transition={{ duration: 0.25, ease: easeOut }}
                style={{
                  height: 8,
                  borderRadius: 4,
                  backgroundColor: isActive ? 'var(--color-accent-primary)' : 'var(--color-border-separator)',
                  transition: 'background-color 0.25s ease',
                }}
              />
            );
          })}
        </div>
      </OnboardingShell>

      <DialogModal
        active={showSkipConfirm}
        title={t('onboarding.tour.skipConfirm.title')}
        description={t('onboarding.tour.skipConfirm.description')}
        confirmText={t('onboarding.tour.skip')}
        closeText={t('common.cancel')}
        onClose={() => setShowSkipConfirm(false)}
        onConfirm={() => {
          setShowSkipConfirm(false);
          const step = slideToStepMap[activeSlide] ?? 'tour-1';
          trackOnboardingEvent('onboarding_skip', { step });
          completeAndNavigate();
        }}
      />
    </>
  );
}
