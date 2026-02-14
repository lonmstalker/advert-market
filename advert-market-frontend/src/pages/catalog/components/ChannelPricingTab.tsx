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
      style={{ padding: '0 16px 16px' }}
    >
      {minPrice != null && (
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, flexWrap: 'wrap' }}>
            <Text type="title1" weight="bold">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                {t('catalog.channel.from', { price: formatTon(minPrice) })}
              </span>
            </Text>
            <Text type="caption1" color="secondary">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(minPrice)}</span>
            </Text>
          </div>
          {heroCpm != null && (
            <div style={{ marginTop: 4 }}>
              <Text type="subheadline2" color="secondary">
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>
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
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {rules.map((rule) => {
        const ruleCpm = channel.avgReach ? computeCpm(rule.priceNano, channel.avgReach) : null;
        const primaryType = rule.postTypes[0] ?? 'NATIVE';
        const localizedType = t(`catalog.channel.postType.${primaryType}`, { defaultValue: rule.name });
        const overlap = buildOverlapLabel(channel.postFrequencyHours, undefined, t);
        return (
          <div key={rule.id} style={pricingCardStyle}>
            <div style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={postTypeIconContainerStyle}>
                <PostTypeIcon
                  postType={primaryType}
                  style={{ width: 20, height: 20, color: 'var(--color-foreground-secondary)' }}
                />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Text type="body" weight="medium">
                    <span
                      style={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        display: 'block',
                      }}
                    >
                      {localizedType}
                    </span>
                  </Text>
                  {rule.description && (
                    <Popover
                      content={
                        <div style={{ whiteSpace: 'pre-wrap', maxWidth: 220 }}>
                          <Text type="caption1" color="secondary">
                            {rule.description}
                          </Text>
                        </div>
                      }
                    >
                      <InfoIcon
                        style={{ width: 14, height: 14, color: 'var(--color-foreground-tertiary)', flexShrink: 0 }}
                      />
                    </Popover>
                  )}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4, flexWrap: 'wrap' }}>
                  {overlap &&
                    (overlap.hasTooltip ? (
                      <Popover
                        content={
                          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <Text type="caption1" color="secondary">
                              {t('catalog.channel.overlapTooltipLine1', { freq: overlap.freq })}
                            </Text>
                            <Text type="caption1" color="secondary">
                              {t('catalog.channel.overlapTooltipLine2', { dur: overlap.dur })}
                            </Text>
                          </div>
                        }
                      >
                        <span style={overlapBadgeStyle}>
                          {overlap.label}
                          <InfoIcon style={{ width: 12, height: 12, color: 'var(--color-foreground-tertiary)' }} />
                        </span>
                      </Popover>
                    ) : (
                      <span style={overlapBadgeStyle}>{overlap.label}</span>
                    ))}
                  {ruleCpm != null && (
                    <span style={cpmBadgeStyle}>
                      <Text type="caption1" color="secondary">
                        {t('catalog.channel.cpmShort', { value: formatCpm(ruleCpm) })}
                      </Text>
                    </span>
                  )}
                </div>
              </div>
              <div style={{ flexShrink: 0, textAlign: 'right' }}>
                <Text type="callout" weight="bold">
                  <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(rule.priceNano)}</span>
                </Text>
                <Text type="caption1" color="tertiary">
                  <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(rule.priceNano)}</span>
                </Text>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

const pricingCardStyle: React.CSSProperties = {
  background: 'var(--color-background-base)',
  border: '1px solid var(--color-border-separator)',
  borderRadius: 12,
  overflow: 'hidden',
};

const postTypeIconContainerStyle: React.CSSProperties = {
  width: 40,
  height: 40,
  borderRadius: 10,
  background: 'var(--color-background-secondary)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  flexShrink: 0,
};

const overlapBadgeStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 4,
  padding: '2px 8px',
  borderRadius: 6,
  background: 'var(--color-background-secondary)',
  fontSize: 12,
  fontWeight: 600,
  color: 'var(--color-foreground-secondary)',
  fontVariantNumeric: 'tabular-nums',
};

const cpmBadgeStyle: React.CSSProperties = {
  display: 'inline-flex',
  padding: '2px 8px',
  borderRadius: 6,
  background: 'var(--color-background-secondary)',
  fontVariantNumeric: 'tabular-nums',
};
