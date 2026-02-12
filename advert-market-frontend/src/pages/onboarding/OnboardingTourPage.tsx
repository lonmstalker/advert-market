import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Text } from '@telegram-tools/ui-kit';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { completeOnboarding } from '@/features/auth';
import { useOnboardingStore } from '@/features/onboarding';
import styles from './OnboardingTourPage.module.css';

const slides = [
  { emoji: '🔍', titleKey: 'onboarding.tour.slide1.title', descKey: 'onboarding.tour.slide1.description' },
  { emoji: '🔒', titleKey: 'onboarding.tour.slide2.title', descKey: 'onboarding.tour.slide2.description' },
  { emoji: '📊', titleKey: 'onboarding.tour.slide3.title', descKey: 'onboarding.tour.slide3.description' },
] as const;

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

  function handleScroll(e: React.UIEvent<HTMLDivElement>) {
    const el = e.currentTarget;
    const index = Math.round(el.scrollLeft / el.clientWidth);
    setActiveSlide(index);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <div className={styles.carousel} onScroll={handleScroll}>
        {slides.map(({ emoji, titleKey, descKey }) => (
          <div key={titleKey} className={styles.slide}>
            <span style={{ fontSize: '64px', lineHeight: 1 }}>{emoji}</span>
            <Text type="title2" weight="bold" style={{ marginTop: '24px' }}>
              {t(titleKey)}
            </Text>
            <Text type="body" color="secondary" style={{ marginTop: '8px', maxWidth: '280px' }}>
              {t(descKey)}
            </Text>
          </div>
        ))}
      </div>

      <div className={styles.dots}>
        {slides.map(({ titleKey }, i) => (
          <div
            key={titleKey}
            className={styles.dot}
            style={{
              backgroundColor: i === activeSlide ? 'var(--color-accent-primary)' : 'var(--color-separator)',
            }}
          />
        ))}
      </div>

      <div style={{ padding: '16px 24px 24px' }}>
        {isLastSlide ? (
          <Button
            text={t('onboarding.tour.finish')}
            type="primary"
            loading={mutation.isPending}
            onClick={() => mutation.mutate()}
            style={{ width: '100%' }}
          />
        ) : (
          <Button
            text={t('onboarding.tour.skip')}
            type="secondary"
            onClick={() => mutation.mutate()}
            style={{ width: '100%' }}
          />
        )}
      </div>
    </div>
  );
}
