import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { EmptyState } from '@/shared/ui';

export default function ProfilePage() {
  const { t } = useTranslation();

  return (
    <div style={{ padding: '16px' }}>
      <Text type="title1" weight="bold">
        {t('profile.title')}
      </Text>
      <EmptyState emoji="👤" title={t('profile.empty.title')} description={t('profile.empty.description')} />
    </div>
  );
}
