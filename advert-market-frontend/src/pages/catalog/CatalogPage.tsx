import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { EmptyState } from '@/shared/ui';

export default function CatalogPage() {
  const { t } = useTranslation();

  return (
    <div style={{ padding: '16px' }}>
      <Text type="title1" weight="bold">
        {t('catalog.title')}
      </Text>
      <EmptyState emoji="🔍" title={t('catalog.empty.title')} description={t('catalog.empty.description')} />
    </div>
  );
}
