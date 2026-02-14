import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { useCountdown } from '@/shared/hooks/use-countdown';
import { formatDate } from '@/shared/lib/date-format';
import { formatFiat } from '@/shared/lib/fiat-format';
import { buildOverlapLabel } from '@/shared/lib/overlap-label';
import { formatTon } from '@/shared/lib/ton-format';
import { Popover } from '@/shared/ui';
import { InfoIcon } from '@/shared/ui/icons';
import type { Deal } from '../types/deal';

type DealInfoCardProps = {
  deal: Deal;
};

export function DealInfoCard({ deal }: DealInfoCardProps) {
  const { t, i18n } = useTranslation();
  const countdown = useCountdown(deal.deadlineAt, t);

  const overlap = buildOverlapLabel(deal.postFrequencyHours, deal.durationHours, t);
  const overlapLabel = overlap?.label ?? null;
  const hasOverlapTooltip = overlap?.hasTooltip ?? false;
  const freq = overlap?.freq;
  const dur = overlap?.dur;

  const chips: { key: string; label: string; tooltip?: boolean }[] = [
    { key: 'type', label: t(`catalog.channel.postType.${deal.postType}`) },
    { key: 'date', label: formatDate(deal.createdAt, i18n.language) },
  ];
  if (overlapLabel) {
    chips.push({ key: 'overlap', label: overlapLabel, tooltip: hasOverlapTooltip });
  }
  if (countdown) {
    chips.push({ key: 'countdown', label: countdown });
  }

  return (
    <div
      style={{
        background: 'var(--color-background-base)',
        border: '1px solid var(--color-border-separator)',
        borderRadius: 12,
        padding: '20px 16px',
        margin: '0 16px',
      }}
    >
      {/* Centered price */}
      <div style={{ textAlign: 'center' }}>
        <Text type="title1" weight="bold">
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(deal.priceNano)}</span>
        </Text>
      </div>
      <div style={{ textAlign: 'center', marginTop: 2 }}>
        <Text type="caption1" color="secondary">
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(deal.priceNano)}</span>
        </Text>
      </div>

      {/* Chips row */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          flexWrap: 'wrap',
          gap: 8,
          marginTop: 14,
        }}
      >
        {chips.map((chip) =>
          chip.tooltip ? (
            <Popover
              key={chip.key}
              content={
                <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <Text type="caption1" color="secondary">
                    {t('catalog.channel.overlapTooltipLine1', { freq })}
                  </Text>
                  <Text type="caption1" color="secondary">
                    {t('catalog.channel.overlapTooltipLine2', { dur })}
                  </Text>
                </div>
              }
            >
              <span style={chipStyle}>
                {chip.label}
                <InfoIcon style={{ width: 12, height: 12, color: 'var(--color-foreground-tertiary)' }} />
              </span>
            </Popover>
          ) : (
            <span key={chip.key} style={chipStyle}>
              {chip.label}
            </span>
          ),
        )}
      </div>

      {/* Deal message */}
      {deal.message && (
        <div
          style={{
            background: 'var(--color-background-secondary)',
            borderRadius: 10,
            padding: '10px 14px',
            marginTop: 12,
          }}
        >
          <div style={{ whiteSpace: 'pre-wrap', fontStyle: 'italic' }}>
            <Text type="caption1" color="secondary">
              {deal.message}
            </Text>
          </div>
        </div>
      )}
    </div>
  );
}

const chipStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 4,
  padding: '4px 10px',
  borderRadius: 8,
  background: 'var(--color-background-secondary)',
  border: '1px solid var(--color-border-separator)',
  fontSize: 13,
  fontWeight: 500,
  color: 'var(--color-foreground-primary)',
  fontVariantNumeric: 'tabular-nums',
};
