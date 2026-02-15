import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { FeatureCard } from '@/features/onboarding/components/feature-card';
import { OnboardingLogo } from '@/features/onboarding/components/onboarding-logo';
import { OnboardingSettingsSheet } from '@/features/onboarding/components/onboarding-settings-sheet';
import { useHaptic } from '@/shared/hooks';
import { DocumentIcon, pressScale, SearchIcon, staggerChildren, Tappable, WalletIcon } from '@/shared/ui';
import { GlobeIcon } from '@/shared/ui/icons';

export default function OnboardingPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const haptic = useHaptic();
  const [showSettings, setShowSettings] = useState(false);

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: 'calc(var(--am-viewport-stable-height) - var(--am-onboarding-top-chrome-height, 40px))',
        padding: '0 24px',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      <Tappable
        onClick={() => setShowSettings(true)}
        style={{
          position: 'absolute',
          top: 8,
          right: 0,
          width: 40,
          height: 40,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderRadius: 10,
          border: 'none',
          background: 'transparent',
          color: 'var(--color-foreground-secondary)',
          cursor: 'pointer',
          WebkitTapHighlightColor: 'transparent',
        }}
        aria-label={t('profile.settings')}
      >
        <GlobeIcon size={20} />
      </Tappable>

      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          gap: '12px',
          minHeight: 0,
          overflowY: 'auto',
        }}
      >
        <OnboardingLogo />

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.15 }}
        >
          <Text type="largeTitle" weight="bold">
            Ad Market
          </Text>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.25 }}
          style={{ maxWidth: '280px' }}
        >
          <Text type="body" color="secondary" align="center">
            {t('onboarding.welcome.subtitle')}
          </Text>
        </motion.div>

        <motion.div
          {...staggerChildren}
          initial="initial"
          animate="animate"
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
            marginTop: '16px',
            width: '100%',
            padding: 10,
            borderRadius: 24,
            background: 'var(--am-hero-gradient-accent)',
          }}
        >
          <FeatureCard
            icon={<SearchIcon size={24} />}
            title={t('onboarding.welcome.featureCard1.title')}
            hint={t('onboarding.welcome.featureCard1.hint')}
          />
          <FeatureCard
            icon={<WalletIcon size={24} />}
            title={t('onboarding.welcome.featureCard2.title')}
            hint={t('onboarding.welcome.featureCard2.hint')}
          />
          <FeatureCard
            icon={<DocumentIcon size={24} />}
            title={t('onboarding.welcome.featureCard3.title')}
            hint={t('onboarding.welcome.featureCard3.hint')}
          />
        </motion.div>
      </div>

      <div style={{ flexShrink: 0, paddingBottom: 'calc(16px + var(--am-safe-area-bottom))', paddingTop: '12px' }}>
        <motion.div {...pressScale}>
          <Button
            text={t('onboarding.welcome.start')}
            type="primary"
            onClick={() => {
              haptic.impactOccurred('light');
              navigate('/onboarding/interest');
            }}
          />
        </motion.div>
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.6 }}
          style={{ textAlign: 'center', marginTop: '12px' }}
        >
          <Text type="caption1" color="secondary">
            {t('onboarding.welcome.timeHint')}
          </Text>
        </motion.div>
      </div>

      <OnboardingSettingsSheet open={showSettings} onClose={() => setShowSettings(false)} />
    </div>
  );
}
