import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { AppPageShell, AppSectionHeader, AppSurfaceCard, BackButtonHandler, LocaleCurrencyEditor } from '@/shared/ui';
import { slideFromRight } from '@/shared/ui/animations';

export default function LocaleCurrencyPage() {
  const { t } = useTranslation();

  return (
    <AppPageShell withTabsPadding={false} testId="profile-locale-currency-page">
      <BackButtonHandler />

      <motion.div {...slideFromRight} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <AppSectionHeader title={t('profile.localeCurrency.title')} subtitle={t('profile.localeCurrency.subtitle')} />
        <AppSurfaceCard>
          <LocaleCurrencyEditor mode="profile" />
        </AppSurfaceCard>
      </motion.div>
    </AppPageShell>
  );
}
