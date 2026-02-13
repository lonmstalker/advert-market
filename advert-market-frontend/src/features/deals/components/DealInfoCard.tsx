import { Text } from '@telegram-tools/ui-kit';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { formatDate } from '@/shared/lib/date-format';
import { formatTon } from '@/shared/lib/ton-format';
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
      const diff = new Date(deadlineAt!).getTime() - Date.now();
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

  const chips: string[] = [t(`catalog.channel.postType.${deal.postType}`), formatDate(deal.createdAt, i18n.language)];
  if (deal.durationHours) {
    chips.push(t('deals.detail.durationValue', { hours: deal.durationHours }));
  }
  if (deal.postFrequencyHours) {
    chips.push(t('deals.detail.postFrequency', { hours: deal.postFrequencyHours }));
  }
  if (countdown) {
    chips.push(countdown);
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
        {chips.map((chip) => (
          <span
            key={chip}
            style={{
              padding: '4px 10px',
              borderRadius: 8,
              background: 'var(--color-background-secondary)',
              border: '1px solid var(--color-border-separator)',
              fontSize: 13,
              fontWeight: 500,
              color: 'var(--color-foreground-primary)',
            }}
          >
            {chip}
          </span>
        ))}
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
