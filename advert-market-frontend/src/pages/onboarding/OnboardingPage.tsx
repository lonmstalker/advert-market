import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { FeatureCard } from '@/features/onboarding/components/feature-card';
import { OnboardingLogo } from '@/features/onboarding/components/onboarding-logo';
import { DocumentIcon, pressScale, SearchIcon, staggerChildren, WalletIcon } from '@/shared/ui';

export default function OnboardingPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: 'calc(100vh - 40px)',
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
          gap: '12px',
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
            gap: '8px',
            marginTop: '24px',
            width: '100%',
          }}
        >
          <FeatureCard
            icon={<SearchIcon width={20} height={20} />}
            title={t('onboarding.welcome.featureCard1.title')}
            hint={t('onboarding.welcome.featureCard1.hint')}
          />
          <FeatureCard
            icon={<WalletIcon width={20} height={20} />}
            title={t('onboarding.welcome.featureCard2.title')}
            hint={t('onboarding.welcome.featureCard2.hint')}
          />
          <FeatureCard
            icon={<DocumentIcon width={20} height={20} />}
            title={t('onboarding.welcome.featureCard3.title')}
            hint={t('onboarding.welcome.featureCard3.hint')}
          />
        </motion.div>
      </div>

      <div style={{ flexShrink: 0, paddingBottom: '32px', paddingTop: '16px' }}>
        <motion.div {...pressScale}>
          <Button
            text={t('onboarding.welcome.start')}
            type="primary"
            onClick={() => navigate('/onboarding/interest')}
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
    </div>
  );
}
