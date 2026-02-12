import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { EmptyState } from '@/shared/ui';

export default function WalletPage() {
  const { t } = useTranslation();

  return (
    <div style={{ padding: '16px' }}>
      <Text type="title1" weight="bold">
        {t('wallet.title')}
      </Text>
      <EmptyState emoji="📜" title={t('wallet.empty.title')} description={t('wallet.empty.description')} />
    </div>
  );
}
