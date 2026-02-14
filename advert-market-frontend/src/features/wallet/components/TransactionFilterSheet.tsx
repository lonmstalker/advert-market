import { Button, Group, GroupItem, Sheet, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { TRANSACTION_TYPES, type TransactionFilters, type TransactionType } from '../types/wallet';

type TransactionFilterSheetProps = {
  open: boolean;
  onClose: () => void;
  filters: TransactionFilters;
  onApply: (filters: TransactionFilters) => void;
  onReset: () => void;
};

export function TransactionFilterSheet({ open, onClose, filters, onApply, onReset }: TransactionFilterSheetProps) {
  const { t } = useTranslation();

  const handleTypeSelect = (type: TransactionType) => {
    onApply({ ...filters, type: filters.type === type ? undefined : type });
  };

  const FiltersSheet = () => (
    <div style={{ padding: '16px' }}>
      <Text type="title2" weight="bold">
        {t('wallet.filters.title')}
      </Text>

      <div style={{ marginTop: 16 }}>
        <div style={{ marginBottom: 8 }}>
          <Text type="caption1" weight="bold" color="secondary">
            {t('wallet.filters.type')}
          </Text>
        </div>
        <Group>
          {TRANSACTION_TYPES.map((type) => (
            <GroupItem
              key={type}
              text={t(`wallet.txType.${typeToKey(type)}`)}
              onClick={() => handleTypeSelect(type)}
              after={
                filters.type === type ? (
                  <Text type="body" color="accent">
                    ✓
                  </Text>
                ) : null
              }
            />
          ))}
        </Group>
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 24 }}>
        <div style={{ flex: 1 }}>
          <Button text={t('wallet.filters.reset')} type="secondary" onClick={onReset} />
        </div>
        <div style={{ flex: 1 }}>
          <Button text={t('wallet.filters.apply')} type="primary" onClick={onClose} />
        </div>
      </div>
    </div>
  );

  return <Sheet sheets={{ filters: FiltersSheet }} activeSheet="filters" opened={open} onClose={onClose} />;
}

function typeToKey(type: TransactionType): string {
  switch (type) {
    case 'escrow_deposit':
      return 'escrowDeposit';
    case 'payout':
      return 'payout';
    case 'refund':
      return 'refund';
    case 'commission':
      return 'commission';
  }
}
