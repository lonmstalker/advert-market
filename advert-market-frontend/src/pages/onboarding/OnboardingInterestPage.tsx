import { Button, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { OnboardingShell } from '@/features/onboarding/components/onboarding-shell';
import { RoleCard } from '@/features/onboarding/components/role-card';
import { RolePreviewItem } from '@/features/onboarding/components/role-preview-item';
import { resolveOnboardingPrimaryRole, useOnboardingStore } from '@/features/onboarding/store/onboarding-store';
import { useHaptic } from '@/shared/hooks';
import { trackOnboardingEvent } from '@/shared/lib/onboarding-analytics';
import { MonitorIcon, pressScale, SpeakerIcon, slideUp } from '@/shared/ui';
import { ChartBarIcon, ClipboardIcon, CoinIcon, HandshakeIcon, SearchIcon, TonDiamondIcon } from '@/shared/ui/icons';

const previewIconClassName = 'w-4 h-4 text-fg-secondary';

export default function OnboardingInterestPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const haptic = useHaptic();
  const { interests, toggleInterest, setActiveSlide } = useOnboardingStore();
  const hasSelection = interests.size > 0;
  const bothSelected = interests.size === 2;

  useEffect(() => {
    trackOnboardingEvent('onboarding_view', { step: 'interest' });
  }, []);

  function handleRoleToggle(role: 'advertiser' | 'owner') {
    const next = new Set(interests);
    if (next.has(role)) {
      next.delete(role);
    } else {
      next.add(role);
    }

    haptic.selectionChanged();
    toggleInterest(role);
    trackOnboardingEvent('role_selected', { role: resolveOnboardingPrimaryRole(next) });
  }

  function handleContinue() {
    trackOnboardingEvent('onboarding_primary_click', { step: 'interest' });
    setActiveSlide(0);
    navigate('/onboarding/tour');
  }

  return (
    <OnboardingShell
      testId="onboarding-interest-step"
      centerContent={false}
      contentClassName="pt-2"
      footer={
        <motion.div {...pressScale}>
          <Button
            text={t('onboarding.interest.continue')}
            type="primary"
            disabled={!hasSelection}
            onClick={handleContinue}
          />
        </motion.div>
      }
    >
      <motion.div {...slideUp}>
        <div className="mb-6">
          <Text type="title1" weight="bold">
            {t('onboarding.interest.title')}
          </Text>
          <div className="mt-2">
            <Text type="body" color="secondary">
              {t('onboarding.interest.subtitle')}
            </Text>
          </div>
        </div>

        <div className="flex-1 flex flex-col gap-4">
          <RoleCard
            icon={<SpeakerIcon width={28} height={28} />}
            title={t('onboarding.interest.advertiser')}
            hint={t('onboarding.interest.advertiserHint')}
            isSelected={interests.has('advertiser')}
            onToggle={() => handleRoleToggle('advertiser')}
          >
            <RolePreviewItem
              icon={<SearchIcon className={previewIconClassName} />}
              text={t('onboarding.interest.advertiserPreview1')}
            />
            <RolePreviewItem
              icon={<CoinIcon className={previewIconClassName} />}
              text={t('onboarding.interest.advertiserPreview2')}
            />
            <RolePreviewItem
              icon={<ChartBarIcon className={previewIconClassName} />}
              text={t('onboarding.interest.advertiserPreview3')}
            />
          </RoleCard>

          <RoleCard
            icon={<MonitorIcon width={28} height={28} />}
            title={t('onboarding.interest.owner')}
            hint={t('onboarding.interest.ownerHint')}
            isSelected={interests.has('owner')}
            onToggle={() => handleRoleToggle('owner')}
          >
            <RolePreviewItem
              icon={<ClipboardIcon className={previewIconClassName} />}
              text={t('onboarding.interest.ownerPreview1')}
            />
            <RolePreviewItem
              icon={<HandshakeIcon className={previewIconClassName} />}
              text={t('onboarding.interest.ownerPreview2')}
            />
            <RolePreviewItem
              icon={<TonDiamondIcon className={previewIconClassName} />}
              text={t('onboarding.interest.ownerPreview3')}
            />
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
                className="text-center py-1"
              >
                <Text type="caption1" color="accent">
                  {t('onboarding.interest.bothRolesHint')}
                </Text>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </motion.div>
    </OnboardingShell>
  );
}
