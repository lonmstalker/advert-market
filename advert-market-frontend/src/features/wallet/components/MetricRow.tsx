import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { formatTon } from '@/shared/lib/ton-format';

type MetricRowProps = {
  escrowAmount: string;
  completedDealsCount: number;
};

export function MetricRow({ escrowAmount, completedDealsCount }: MetricRowProps) {
  const { t } = useTranslation();

  return (
    <div className="am-metric-row am-finance-card overflow-hidden">
      <div className="am-metric-row__cell">
        <Text type="title3" weight="bold">
          <span className="am-tabnum">{formatTon(escrowAmount)}</span>
        </Text>
        <div className="mt-1.5">
          <Text type="caption1" color="secondary">
            {t('wallet.stats.inEscrow')}
          </Text>
        </div>
      </div>

      <div data-testid="metric-divider" className="am-metric-row__divider" />

      <div className="am-metric-row__cell">
        <Text type="title3" weight="bold">
          {completedDealsCount}
        </Text>
        <div className="mt-1.5">
          <Text type="caption1" color="secondary">
            {t('wallet.stats.completedDeals')}
          </Text>
        </div>
      </div>
    </div>
  );
}
