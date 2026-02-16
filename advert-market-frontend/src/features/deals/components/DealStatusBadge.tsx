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
      className="inline-flex items-center px-2.5 py-0.5 rounded-[8px] whitespace-nowrap"
      style={{ backgroundColor: statusBgVar(config.color) }}
    >
      <Text type="caption1" weight="bold">
        <span style={{ color: statusColorVar(config.color) }}>{t(config.i18nKey)}</span>
      </Text>
    </span>
  );
}
