import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Text } from '@telegram-tools/ui-kit';
import { easeOut } from 'motion';
import { AnimatePresence, motion } from 'motion/react';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { completeOnboarding } from '@/features/auth';
import { useOnboardingStore } from '@/features/onboarding';
import { TourSlideCatalog } from '@/features/onboarding/components/tour-slide-catalog';
import { TourSlideDeal } from '@/features/onboarding/components/tour-slide-deal';
import { TourSlideWallet } from '@/features/onboarding/components/tour-slide-wallet';
import { profileKeys } from '@/shared/api';
import { pressScale } from '@/shared/ui';

const SLIDE_COUNT = 3;

const slideVariants = {
  enter: { opacity: 0, x: 40 },
  center: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: -40 },
};

const slideComponents = [TourSlideCatalog, TourSlideDeal, TourSlideWallet];

export default function OnboardingTourPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { interests, tourTasksCompleted, reset } = useOnboardingStore();
  const [activeSlide, setActiveSlide] = useState(0);
  const [showError, setShowError] = useState(false);
  const isAnimating = useRef(false);
  const hasCompleted = useRef(false);

  useEffect(() => {
    if (interests.size === 0 && !hasCompleted.current) {
      navigate('/onboarding/interest', { replace: true });
    }
  }, [interests.size, navigate]);

  const mutation = useMutation({
    mutationFn: () => completeOnboarding([...interests]),
    onSuccess: (updatedProfile) => {
      hasCompleted.current = true;
      queryClient.setQueryData(profileKeys.me, updatedProfile);
      navigate('/catalog', { replace: true });
      reset();
    },
    onError: () => {
      setShowError(true);
    },
  });

  const isLastSlide = activeSlide === SLIDE_COUNT - 1;
  const taskDone = tourTasksCompleted.has(activeSlide);

  function handleNext() {
    if (isAnimating.current || mutation.isPending) return;

    if (isLastSlide) {
      setShowError(false);
      mutation.mutate();
    } else {
      isAnimating.current = true;
      setActiveSlide((prev) => prev + 1);
    }
  }

  function handleSkip() {
    if (mutation.isPending) return;
    setShowError(false);
    mutation.mutate();
  }

  function handleAnimationComplete() {
    isAnimating.current = false;
  }

  const SlideComponent = slideComponents[activeSlide]!;

  if (interests.size === 0) return null;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: 'calc(100vh - 40px)',
        padding: '0 16px',
      }}
    >
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          position: 'relative',
          overflow: 'hidden',
          padding: '8px 0',
        }}
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
            <SlideComponent />
          </motion.div>
        </AnimatePresence>
      </div>

      <div
        role="tablist"
        aria-label={t('onboarding.tour.next')}
        style={{
          display: 'flex',
          justifyContent: 'center',
          gap: '6px',
          paddingBottom: '16px',
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

      <div
        style={{
          flexShrink: 0,
          paddingBottom: '32px',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
        }}
      >
        <motion.div {...pressScale}>
          <Button
            text={isLastSlide ? t('onboarding.tour.finish') : t('onboarding.tour.next')}
            type="primary"
            loading={mutation.isPending}
            disabled={!isLastSlide && !taskDone}
            onClick={handleNext}
          />
        </motion.div>

        <AnimatePresence>
          {!isLastSlide && !taskDone && (
            <motion.div
              role="status"
              aria-live="polite"
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.2 }}
              style={{ textAlign: 'center', overflow: 'hidden' }}
            >
              <Text type="caption1" color="secondary">
                {t('onboarding.tour.taskRequired')}
              </Text>
            </motion.div>
          )}
        </AnimatePresence>

        <AnimatePresence>
          {showError && (
            <motion.div
              role="alert"
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.2 }}
              style={{ textAlign: 'center', overflow: 'hidden' }}
            >
              <Text type="caption1" color="danger">
                {t('onboarding.tour.error')}
              </Text>
            </motion.div>
          )}
        </AnimatePresence>

        {!isLastSlide && (
          <button
            type="button"
            className="focusable"
            onClick={handleSkip}
            disabled={mutation.isPending}
            style={{
              textAlign: 'center',
              padding: '8px',
              cursor: 'pointer',
              WebkitTapHighlightColor: 'transparent',
              background: 'none',
              border: 'none',
            }}
          >
            <Text type="subheadline1" color="secondary" align="center">
              {t('onboarding.tour.skip')}
            </Text>
          </button>
        )}
      </div>
    </div>
  );
}
