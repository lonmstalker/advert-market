import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useOnboardingStore } from '@/features/onboarding';
import { slideUp } from '@/shared/ui';

type Interest = 'advertiser' | 'owner';

const interestCards: { key: Interest; emoji: string; labelKey: string; hintKey: string }[] = [
  {
    key: 'advertiser',
    emoji: '📣',
    labelKey: 'onboarding.interest.advertiser',
    hintKey: 'onboarding.interest.advertiserHint',
  },
  { key: 'owner', emoji: '📺', labelKey: 'onboarding.interest.owner', hintKey: 'onboarding.interest.ownerHint' },
];

export default function OnboardingInterestPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { interests, toggleInterest } = useOnboardingStore();
  const hasSelection = interests.size > 0;

  return (
    <motion.div
      {...slideUp}
      style={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        padding: '24px',
      }}
    >
      <div style={{ flex: 1 }}>
        <Text type="title1" weight="bold">
          {t('onboarding.interest.title')}
        </Text>
        <Text type="body" color="secondary" style={{ marginTop: '8px' }}>
          {t('onboarding.interest.subtitle')}
        </Text>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '32px' }}>
          {interestCards.map(({ key, emoji, labelKey, hintKey }) => {
            const isSelected = interests.has(key);
            return (
              <button
                key={key}
                type="button"
                onClick={() => toggleInterest(key)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '16px',
                  padding: '16px',
                  borderRadius: '12px',
                  border: `2px solid ${isSelected ? 'var(--color-accent-primary)' : 'var(--color-separator)'}`,
                  backgroundColor: isSelected ? 'var(--color-accent-primary-alpha)' : 'var(--color-background-primary)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  width: '100%',
                  transition: 'all 0.15s ease',
                }}
              >
                <span style={{ fontSize: '32px', lineHeight: 1 }}>{emoji}</span>
                <div>
                  <Text type="title3" weight="semibold">
                    {t(labelKey)}
                  </Text>
                  <Text type="caption1" color="secondary">
                    {t(hintKey)}
                  </Text>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      <Button
        text={t('onboarding.interest.continue')}
        type="primary"
        disabled={!hasSelection}
        onClick={() => navigate('/onboarding/tour')}
        style={{ width: '100%', marginTop: '24px' }}
      />
    </motion.div>
  );
}
