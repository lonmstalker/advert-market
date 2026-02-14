import { Button, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useOnboardingStore } from '@/features/onboarding';
import { RoleCard } from '@/features/onboarding/components/role-card';
import { RolePreviewItem } from '@/features/onboarding/components/role-preview-item';
import { MonitorIcon, pressScale, SpeakerIcon, slideUp } from '@/shared/ui';

export default function OnboardingInterestPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { interests, toggleInterest } = useOnboardingStore();
  const hasSelection = interests.size > 0;
  const bothSelected = interests.size === 2;

  return (
    <motion.div
      {...slideUp}
      style={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: 'calc(100vh - 40px)',
        padding: '16px',
      }}
    >
      <div style={{ marginBottom: '24px' }}>
        <Text type="title1" weight="bold">
          {t('onboarding.interest.title')}
        </Text>
        <div style={{ marginTop: '8px' }}>
          <Text type="body" color="secondary">
            {t('onboarding.interest.subtitle')}
          </Text>
        </div>
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <RoleCard
          icon={<SpeakerIcon width={28} height={28} />}
          title={t('onboarding.interest.advertiser')}
          hint={t('onboarding.interest.advertiserHint')}
          isSelected={interests.has('advertiser')}
          onToggle={() => toggleInterest('advertiser')}
        >
          <RolePreviewItem emoji="🔍" text={t('onboarding.interest.advertiserPreview1')} />
          <RolePreviewItem emoji="💰" text={t('onboarding.interest.advertiserPreview2')} />
          <RolePreviewItem emoji="📊" text={t('onboarding.interest.advertiserPreview3')} />
        </RoleCard>

        <RoleCard
          icon={<MonitorIcon width={28} height={28} />}
          title={t('onboarding.interest.owner')}
          hint={t('onboarding.interest.ownerHint')}
          isSelected={interests.has('owner')}
          onToggle={() => toggleInterest('owner')}
        >
          <RolePreviewItem emoji="📋" text={t('onboarding.interest.ownerPreview1')} />
          <RolePreviewItem emoji="🤝" text={t('onboarding.interest.ownerPreview2')} />
          <RolePreviewItem emoji="💎" text={t('onboarding.interest.ownerPreview3')} />
        </RoleCard>

        <AnimatePresence>
          {bothSelected && (
            <motion.div
              role="status"
              aria-live="polite"
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.2 }}
              style={{ textAlign: 'center', padding: '4px 0' }}
            >
              <Text type="caption1" color="accent">
                {t('onboarding.interest.bothRolesHint')}
              </Text>
            </motion.div>
          )}
        </AnimatePresence>
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
