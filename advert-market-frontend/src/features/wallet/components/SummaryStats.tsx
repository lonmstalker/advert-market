import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { formatTon } from '@/shared/lib/ton-format';
import type { WalletSummary } from '../types/wallet';

type SummaryStatsProps = {
  summary: WalletSummary;
};

function isOwnerView(summary: WalletSummary): boolean {
  return summary.earnedTotalNano !== '0';
}

export function SummaryStats({ summary }: SummaryStatsProps) {
  const { t } = useTranslation();
  const isOwner = isOwnerView(summary);

  const escrowAmount = isOwner ? summary.inEscrowNano : summary.activeEscrowNano;

  return (
    <Group>
      <GroupItem
        text={t('wallet.stats.inEscrow')}
        after={
          <Text type="body" weight="medium">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(escrowAmount)}</span>
          </Text>
        }
      />
      <GroupItem
        text={t('wallet.stats.completedDeals')}
        after={
          <Text type="body" weight="medium">
            {summary.completedDealsCount}
          </Text>
        }
      />
      <GroupItem
        text={t('wallet.stats.activeDeals')}
        after={
          <Text type="body" weight="medium">
            {summary.activeDealsCount}
          </Text>
        }
      />
    </Group>
  );
}
