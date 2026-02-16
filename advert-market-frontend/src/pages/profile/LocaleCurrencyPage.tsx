import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { LocaleCurrencyEditor } from '@/features/profile/components/LocaleCurrencyEditor';
import { BackButtonHandler } from '@/shared/ui';
import { slideFromRight } from '@/shared/ui/animations';

export default function LocaleCurrencyPage() {
  const { t } = useTranslation();

  return (
    <motion.div {...slideFromRight} style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <BackButtonHandler />

      <div>
        <Text type="title1" weight="bold">
          {t('profile.localeCurrency.title')}
        </Text>
        <div style={{ marginTop: 6 }}>
          <Text type="caption1" color="secondary">
            {t('profile.localeCurrency.subtitle')}
          </Text>
        </div>
      </div>

      <LocaleCurrencyEditor mode="profile" />
    </motion.div>
  );
}
