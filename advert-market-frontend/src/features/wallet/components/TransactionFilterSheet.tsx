import { Button, Sheet, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { ToggleChip } from '@/shared/ui';
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

  const handleApply = () => {
    onClose();
  };

  const FiltersSheet = () => (
    <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Text type="title2" weight="bold">
        {t('wallet.filters.title')}
      </Text>

      <div>
        <div style={{ marginBottom: 8 }}>
          <Text type="body" weight="medium">
            {t('wallet.filters.type')}
          </Text>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {TRANSACTION_TYPES.map((type) => (
            <ToggleChip
              key={type}
              label={t(`wallet.txType.${typeToKey(type)}`)}
              active={filters.type === type}
              onClick={() => handleTypeSelect(type)}
            />
          ))}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <div style={{ flex: 1 }}>
          <Button text={t('wallet.filters.reset')} type="secondary" onClick={onReset} />
        </div>
        <div style={{ flex: 1 }}>
          <Button text={t('wallet.filters.apply')} type="primary" onClick={handleApply} />
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
