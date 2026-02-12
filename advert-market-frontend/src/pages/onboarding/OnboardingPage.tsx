import { Button, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { fadeIn } from '@/shared/ui';

export default function OnboardingPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <motion.div
      {...fadeIn}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: '24px',
        textAlign: 'center',
        gap: '16px',
      }}
    >
      <span style={{ fontSize: '80px', lineHeight: 1 }}>📢</span>

      <Text type="title1" weight="bold">
        {t('onboarding.welcome.title')}
      </Text>

      <Text type="body" color="secondary" style={{ maxWidth: '280px' }}>
        {t('onboarding.welcome.subtitle')}
      </Text>

      <Button
        text={t('onboarding.welcome.start')}
        type="primary"
        onClick={() => navigate('/onboarding/interest')}
        style={{ marginTop: '24px', width: '100%', maxWidth: '320px' }}
      />
    </motion.div>
  );
}
