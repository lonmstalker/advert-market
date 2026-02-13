import { Text } from '@telegram-tools/ui-kit';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { formatDate } from '@/shared/lib/date-format';
import { formatFiat } from '@/shared/lib/fiat-format';
import { formatTon } from '@/shared/lib/ton-format';
import { Popover } from '@/shared/ui';
import { InfoIcon } from '@/shared/ui/icons';
import type { Deal } from '../types/deal';

type DealInfoCardProps = {
  deal: Deal;
};

function useCountdown(
  deadlineAt: string | null,
  t: (key: string, opts?: Record<string, unknown>) => string,
): string | null {
  const [remaining, setRemaining] = useState<string | null>(null);

  useEffect(() => {
    if (!deadlineAt) {
      setRemaining(null);
      return;
    }

    function update() {
      const diff = new Date(deadlineAt as string).getTime() - Date.now();
      if (diff <= 0) {
        setRemaining(null);
        return;
      }
      const totalHours = Math.floor(diff / 3_600_000);
      const minutes = Math.floor((diff % 3_600_000) / 60_000);
      if (totalHours >= 24) {
        const days = Math.floor(totalHours / 24);
        const hours = totalHours % 24;
        setRemaining(t('deals.detail.deadlineDays', { days, hours }));
      } else if (totalHours > 0) {
        setRemaining(t('deals.detail.deadlineHours', { hours: totalHours, minutes }));
      } else {
        setRemaining(t('deals.detail.deadlineMinutes', { minutes }));
      }
    }

    update();
    const interval = setInterval(update, 60_000);
    return () => clearInterval(interval);
  }, [deadlineAt, t]);

  return remaining;
}

export function DealInfoCard({ deal }: DealInfoCardProps) {
  const { t, i18n } = useTranslation();
  const countdown = useCountdown(deal.deadlineAt, t);

  const freq = deal.postFrequencyHours;
  const dur = deal.durationHours;
  const overlapLabel =
    freq && dur
      ? t('catalog.channel.overlapFormat', { freq, dur })
      : dur
        ? t('catalog.channel.onlyDuration', { dur })
        : freq
          ? t('catalog.channel.onlyFrequency', { freq })
          : null;
  const hasOverlapTooltip = !!(freq && dur);

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
      <Text type="title1" weight="bold" style={{ textAlign: 'center', display: 'block' }}>
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(deal.priceNano)}</span>
      </Text>
      <Text type="caption1" color="secondary" style={{ textAlign: 'center', display: 'block', marginTop: 2 }}>
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(deal.priceNano)}</span>
      </Text>

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
          <Text type="caption1" color="secondary" style={{ whiteSpace: 'pre-wrap', fontStyle: 'italic' }}>
            {deal.message}
          </Text>
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
