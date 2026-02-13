import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { EmptyState } from '@/shared/ui';

export default function WalletPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div style={{ padding: '16px' }}>
      <Text type="title1" weight="bold">
        {t('wallet.title')}
      </Text>
      <EmptyState
        emoji="📜"
        title={t('wallet.empty.title')}
        description={t('wallet.empty.description')}
        actionLabel={t('wallet.empty.cta')}
        onAction={() => navigate('/catalog')}
      />
    </div>
  );
}
