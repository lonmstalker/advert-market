import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { completeOnboarding } from '@/features/auth';
import { useOnboardingStore } from '@/features/onboarding';
import { pressScale } from '@/shared/ui';

const slides = [
  { emoji: '🔍', titleKey: 'onboarding.tour.slide1.title', descKey: 'onboarding.tour.slide1.description' },
  { emoji: '🔒', titleKey: 'onboarding.tour.slide2.title', descKey: 'onboarding.tour.slide2.description' },
  { emoji: '📊', titleKey: 'onboarding.tour.slide3.title', descKey: 'onboarding.tour.slide3.description' },
] as const;

const slideVariants = {
  enter: { opacity: 0, x: 40 },
  center: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: -40 },
};

export default function OnboardingTourPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { interests, reset } = useOnboardingStore();
  const [activeSlide, setActiveSlide] = useState(0);

  const mutation = useMutation({
    mutationFn: () => completeOnboarding([...interests]),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['auth', 'profile'] });
      reset();
      navigate('/catalog', { replace: true });
    },
  });

  const isLastSlide = activeSlide === slides.length - 1;
  const current = slides[activeSlide];

  function handleNext() {
    if (isLastSlide) {
      mutation.mutate();
    } else {
      setActiveSlide((prev) => prev + 1);
    }
  }

  function handleSkip() {
    mutation.mutate();
  }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        padding: '0 24px',
      }}
    >
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <AnimatePresence mode="wait">
          <motion.div
            key={activeSlide}
            variants={slideVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ duration: 0.25, ease: 'easeOut' }}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '16px',
            }}
          >
            <div
              style={{
                width: '80px',
                height: '80px',
                borderRadius: '24px',
                backgroundColor: 'var(--color-background-base)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <span style={{ fontSize: '40px', lineHeight: 1 }}>{current.emoji}</span>
            </div>

            <Text type="title2" weight="bold">
              {t(current.titleKey)}
            </Text>

            <Text type="body" color="secondary" align="center" style={{ maxWidth: '280px' }}>
              {t(current.descKey)}
            </Text>
          </motion.div>
        </AnimatePresence>
      </div>

      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          gap: '6px',
          paddingBottom: '24px',
        }}
      >
        {slides.map((slide, i) => {
          const isActive = i === activeSlide;
          return (
            <motion.div
              key={slide.titleKey}
              animate={{ width: isActive ? 24 : 8 }}
              transition={{ duration: 0.25, ease: 'easeOut' }}
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
            onClick={handleNext}
          />
        </motion.div>

        {!isLastSlide && (
          <motion.div
            {...pressScale}
            style={{
              textAlign: 'center',
              padding: '8px',
              cursor: 'pointer',
              WebkitTapHighlightColor: 'transparent',
            }}
            onClick={handleSkip}
          >
            <Text type="subheadline1" color="secondary" align="center">
              {t('onboarding.tour.skip')}
            </Text>
          </motion.div>
        )}
      </div>
    </div>
  );
}
