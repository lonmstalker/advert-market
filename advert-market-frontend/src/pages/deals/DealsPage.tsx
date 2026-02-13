import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { EmptyState } from '@/shared/ui';

export default function DealsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div style={{ padding: '16px' }}>
      <Text type="title1" weight="bold">
        {t('deals.title')}
      </Text>
      <EmptyState
        emoji="📬"
        title={t('deals.empty.title')}
        description={t('deals.empty.description')}
        actionLabel={t('deals.empty.cta')}
        onAction={() => navigate('/catalog')}
      />
    </div>
  );
}
