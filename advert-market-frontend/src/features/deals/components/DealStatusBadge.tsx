import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { getStatusConfig, statusBgVar, statusColorVar } from '../lib/deal-status';
import type { DealStatus } from '../types/deal';

type DealStatusBadgeProps = {
  status: DealStatus;
};

export function DealStatusBadge({ status }: DealStatusBadgeProps) {
  const { t } = useTranslation();
  const config = getStatusConfig(status);

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: 6,
        backgroundColor: statusBgVar(config.color),
        whiteSpace: 'nowrap',
      }}
    >
      <span style={{ color: statusColorVar(config.color), fontSize: 12 }}>
        <Text type="caption1" weight="bold">
          {t(config.i18nKey)}
        </Text>
      </span>
    </span>
  );
}
