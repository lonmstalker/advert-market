import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { pressScale, staggerChildren } from '@/shared/ui';

const features = [
  { emoji: '🔍', key: 'onboarding.welcome.feature1' },
  { emoji: '🔒', key: 'onboarding.welcome.feature2' },
  { emoji: '📊', key: 'onboarding.welcome.feature3' },
] as const;

const featureItem = {
  initial: { opacity: 0, y: 8 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.25 },
};

export default function OnboardingPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

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
          gap: '12px',
        }}
      >
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4, ease: 'easeOut' }}
          style={{
            width: '80px',
            height: '80px',
            borderRadius: '24px',
            backgroundColor: 'var(--color-background-base)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '8px',
          }}
        >
          <span style={{ fontSize: '40px', lineHeight: 1 }}>📢</span>
        </motion.div>

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
            marginTop: '24px',
            width: '100%',
            maxWidth: '280px',
          }}
        >
          {features.map(({ emoji, key }) => (
            <motion.div
              key={key}
              {...featureItem}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                textAlign: 'left',
              }}
            >
              <span style={{ fontSize: '20px', lineHeight: 1, flexShrink: 0 }}>{emoji}</span>
              <Text type="subheadline1" color="secondary">
                {t(key)}
              </Text>
            </motion.div>
          ))}
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
      </div>
    </div>
  );
}
