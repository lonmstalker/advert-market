import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import type { ChannelDetail, PricingRule } from '@/features/channels';
import { formatFiat } from '@/shared/lib/fiat-format';
import { buildOverlapLabel } from '@/shared/lib/overlap-label';
import { computeCpm, formatCpm, formatTon } from '@/shared/lib/ton-format';
import { Popover } from '@/shared/ui';
import { InfoIcon, PostTypeIcon } from '@/shared/ui/icons';

type ChannelPricingTabProps = {
  channel: ChannelDetail;
  minPrice: number | null;
  heroCpm: number | null;
};

export function ChannelPricingTab({ channel, minPrice, heroCpm }: ChannelPricingTabProps) {
  const { t } = useTranslation();

  return (
    <motion.div
      key="pricing"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.15 }}
      className="px-4 pb-5"
    >
      {minPrice != null && (
        <div className="mb-5">
          <div className="flex items-baseline gap-2 flex-wrap">
            <Text type="title1" weight="bold">
              <span className="am-tabnum">{t('catalog.channel.from', { price: formatTon(minPrice) })}</span>
            </Text>
            <Text type="caption1" color="secondary">
              <span className="am-tabnum">{formatFiat(minPrice)}</span>
            </Text>
          </div>
          {heroCpm != null && (
            <div className="mt-1">
              <Text type="subheadline2" color="secondary">
                <span className="am-tabnum">
                  {'\u2248 '}
                  {formatCpm(heroCpm)} TON {t('catalog.channel.perThousandViews')}
                </span>
              </Text>
            </div>
          )}
        </div>
      )}

      <PricingRulesList rules={channel.pricingRules} channel={channel} />
    </motion.div>
  );
}

function PricingRulesList({ rules, channel }: { rules: PricingRule[]; channel: ChannelDetail }) {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col gap-3.5">
      {rules.map((rule) => {
        const ruleCpm = channel.avgReach ? computeCpm(rule.priceNano, channel.avgReach) : null;
        const primaryType = rule.postTypes[0] ?? 'NATIVE';
        const localizedType = t(`catalog.channel.postType.${primaryType}`, { defaultValue: rule.name });
        const overlap = buildOverlapLabel(channel.postFrequencyHours, undefined, t);
        return (
          <div key={rule.id} className="bg-bg-base border border-separator rounded-[14px] overflow-hidden">
            <div className="py-4 px-4 flex items-center gap-3.5">
              <div className="w-10 h-10 rounded-[10px] bg-bg-secondary flex items-center justify-center shrink-0">
                <PostTypeIcon postType={primaryType} className="w-5 h-5 text-fg-secondary" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1.5">
                  <Text type="body" weight="medium">
                    <span className="am-truncate">{localizedType}</span>
                  </Text>
                  {rule.description && (
                    <Popover
                      content={
                        <div className="whitespace-pre-wrap max-w-[220px]">
                          <Text type="caption1" color="secondary">
                            {rule.description}
                          </Text>
                        </div>
                      }
                    >
                      <InfoIcon className="w-3.5 h-3.5 text-fg-tertiary shrink-0" />
                    </Popover>
                  )}
                </div>
                <div className="flex items-center gap-1.5 mt-1 flex-wrap">
                  {overlap &&
                    (overlap.hasTooltip ? (
                      <Popover
                        content={
                          <div className="flex flex-col gap-0.5">
                            <Text type="caption1" color="secondary">
                              {t('catalog.channel.overlapTooltipLine1', { freq: overlap.freq })}
                            </Text>
                            <Text type="caption1" color="secondary">
                              {t('catalog.channel.overlapTooltipLine2', { dur: overlap.dur })}
                            </Text>
                          </div>
                        }
                      >
                        <span className="am-pricing-badge">
                          {overlap.label}
                          <InfoIcon className="w-3 h-3 text-fg-tertiary" />
                        </span>
                      </Popover>
                    ) : (
                      <span className="am-pricing-badge">{overlap.label}</span>
                    ))}
                  {ruleCpm != null && (
                    <span className="am-pricing-badge">
                      <Text type="caption1" color="secondary">
                        {t('catalog.channel.cpmShort', { value: formatCpm(ruleCpm) })}
                      </Text>
                    </span>
                  )}
                </div>
              </div>
              <div className="shrink-0 text-right">
                <Text type="callout" weight="bold">
                  <span className="am-tabnum">{formatTon(rule.priceNano)}</span>
                </Text>
                <Text type="caption1" color="tertiary">
                  <span className="am-tabnum">{formatFiat(rule.priceNano)}</span>
                </Text>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
