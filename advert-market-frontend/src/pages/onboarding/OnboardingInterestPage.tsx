import { Button, Icon, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useOnboardingStore } from '@/features/onboarding';
import { pressScale, slideUp } from '@/shared/ui';

type Interest = 'advertiser' | 'owner';

const interestCards: { key: Interest; emoji: string; labelKey: string; hintKey: string }[] = [
  {
    key: 'advertiser',
    emoji: '📣',
    labelKey: 'onboarding.interest.advertiser',
    hintKey: 'onboarding.interest.advertiserHint',
  },
  {
    key: 'owner',
    emoji: '📺',
    labelKey: 'onboarding.interest.owner',
    hintKey: 'onboarding.interest.ownerHint',
  },
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
        padding: '16px',
      }}
    >
      <div style={{ marginBottom: '24px' }}>
        <Text type="title1" weight="bold">
          {t('onboarding.interest.title')}
        </Text>
        <Text type="body" color="secondary" style={{ marginTop: '8px' }}>
          {t('onboarding.interest.subtitle')}
        </Text>
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {interestCards.map(({ key, emoji, labelKey, hintKey }) => {
          const isSelected = interests.has(key);
          return (
            <motion.div key={key} {...pressScale}>
              <button
                type="button"
                onClick={() => toggleInterest(key)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '16px',
                  padding: '16px',
                  borderRadius: '12px',
                  border: `1px solid ${isSelected ? 'var(--color-accent-primary)' : 'var(--color-border-separator)'}`,
                  backgroundColor: isSelected
                    ? 'rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.08)'
                    : 'var(--color-background-base)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  width: '100%',
                  transition: 'border-color 0.15s ease, background-color 0.15s ease',
                  WebkitTapHighlightColor: 'transparent',
                  outline: 'none',
                }}
              >
                <div
                  style={{
                    width: '48px',
                    height: '48px',
                    borderRadius: '12px',
                    backgroundColor: 'var(--color-background-secondary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                  }}
                >
                  <span style={{ fontSize: '24px', lineHeight: 1 }}>{emoji}</span>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <Text type="body" weight="medium">
                    {t(labelKey)}
                  </Text>
                  <Text type="caption1" color="secondary">
                    {t(hintKey)}
                  </Text>
                </div>
                <AnimatePresence>
                  {isSelected && (
                    <motion.div
                      initial={{ opacity: 0, scale: 0.5 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.5 }}
                      transition={{ duration: 0.15 }}
                      style={{ flexShrink: 0 }}
                    >
                      <Icon name="check" color="accent" size="24px" />
                    </motion.div>
                  )}
                </AnimatePresence>
              </button>
            </motion.div>
          );
        })}
      </div>

      <div style={{ flexShrink: 0, paddingBottom: '32px', paddingTop: '16px' }}>
        <motion.div {...pressScale}>
          <Button
            text={t('onboarding.interest.continue')}
            type="primary"
            disabled={!hasSelection}
            onClick={() => navigate('/onboarding/tour')}
          />
        </motion.div>
      </div>
    </motion.div>
  );
}
