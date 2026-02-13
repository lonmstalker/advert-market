import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { formatTon } from '@/shared/lib/ton-format';
import type { PricingRule } from '../types/channel';

type PricingRulesListProps = {
  pricingRules: PricingRule[];
};

export function PricingRulesList({ pricingRules }: PricingRulesListProps) {
  const { t } = useTranslation();

  return (
    <Group header={t('catalog.channel.pricing')}>
      {pricingRules.map((rule) => (
        <GroupItem
          key={rule.id}
          text={rule.name}
          after={
            <Text type="callout" weight="medium">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(rule.priceNano)}</span>
            </Text>
          }
        />
      ))}
    </Group>
  );
}
