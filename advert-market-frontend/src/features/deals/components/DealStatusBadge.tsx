import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { getStatusConfig, statusBgClass, statusTextClass } from '../lib/deal-status';
import type { DealStatus } from '../types/deal';

type DealStatusBadgeProps = {
  status: DealStatus;
};

export function DealStatusBadge({ status }: DealStatusBadgeProps) {
  const { t } = useTranslation();
  const config = getStatusConfig(status);

  return (
    <span
      data-testid="deal-status-badge"
      className={`inline-flex items-center px-2.5 py-1 rounded-[8px] whitespace-nowrap ${statusBgClass(config.color)}`}
    >
      <Text type="caption1" weight="bold">
        <span className={statusTextClass(config.color)}>{t(config.i18nKey)}</span>
      </Text>
    </span>
  );
}
