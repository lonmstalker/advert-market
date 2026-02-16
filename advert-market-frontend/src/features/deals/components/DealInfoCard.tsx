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
    <div className="bg-bg-base border border-separator rounded-control px-4 py-6 mx-4">
      {/* Centered price */}
      <div className="text-center">
        <Text type="title1" weight="bold">
          <span className="am-tabnum">{formatTon(deal.priceNano)}</span>
        </Text>
      </div>
      <div className="text-center mt-0.5">
        <Text type="caption1" color="secondary">
          <span className="am-tabnum">{formatFiat(deal.priceNano)}</span>
        </Text>
      </div>

      {/* Chips row */}
      <div className="flex justify-center flex-wrap gap-2 mt-4">
        {chips.map((chip) =>
          chip.tooltip ? (
            <Popover
              key={chip.key}
              content={
                <div className="flex flex-col gap-0.5">
                  <Text type="caption1" color="secondary">
                    {t('catalog.channel.overlapTooltipLine1', { freq })}
                  </Text>
                  <Text type="caption1" color="secondary">
                    {t('catalog.channel.overlapTooltipLine2', { dur })}
                  </Text>
                </div>
              }
            >
              <span className="am-deal-info-chip">
                {chip.label}
                <InfoIcon className="size-3 text-fg-tertiary" />
              </span>
            </Popover>
          ) : (
            <span key={chip.key} className="am-deal-info-chip">
              {chip.label}
            </span>
          ),
        )}
      </div>

      {/* Deal message */}
      {deal.message && (
        <div className="bg-bg-secondary rounded-[10px] px-3.5 py-3 mt-4">
          <div className="whitespace-pre-wrap italic">
            <Text type="caption1" color="secondary">
              {deal.message}
            </Text>
          </div>
        </div>
      )}
    </div>
  );
}
