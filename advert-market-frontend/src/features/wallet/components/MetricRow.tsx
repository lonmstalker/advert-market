import { Text } from '@telegram-tools/ui-kit';
import type React from 'react';
import { useTranslation } from 'react-i18next';
import { formatTon } from '@/shared/lib/ton-format';

type MetricRowProps = {
  escrowAmount: string;
  completedDealsCount: number;
};

const containerStyle: React.CSSProperties = {
  display: 'flex',
  overflow: 'hidden',
  borderRadius: 18,
};

const cellStyle: React.CSSProperties = {
  flex: 1,
  padding: '16px 12px',
  textAlign: 'center',
};

const dividerStyle: React.CSSProperties = {
  width: 1,
  alignSelf: 'stretch',
  background: 'var(--am-card-border)',
};

export function MetricRow({ escrowAmount, completedDealsCount }: MetricRowProps) {
  const { t } = useTranslation();

  return (
    <div className="am-finance-card" style={containerStyle}>
      <div style={cellStyle}>
        <Text type="title3" weight="bold">
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(escrowAmount)}</span>
        </Text>
        <div style={{ marginTop: 2 }}>
          <Text type="caption1" color="secondary">
            {t('wallet.stats.inEscrow')}
          </Text>
        </div>
      </div>

      <div data-testid="metric-divider" style={dividerStyle} />

      <div style={cellStyle}>
        <Text type="title3" weight="bold">
          {completedDealsCount}
        </Text>
        <div style={{ marginTop: 2 }}>
          <Text type="caption1" color="secondary">
            {t('wallet.stats.completedDeals')}
          </Text>
        </div>
      </div>
    </div>
  );
}
