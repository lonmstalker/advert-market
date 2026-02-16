import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { DealStatusBadge } from '@/features/deals/components/DealStatusBadge';
import type { StatusConfig } from '@/features/deals/lib/deal-status';
import { statusBgClass } from '@/features/deals/lib/deal-status';
import type { Deal } from '@/features/deals/types/deal';
import { formatDate } from '@/shared/lib/date-format';
import { formatFiat } from '@/shared/lib/fiat-format';
import { buildOverlapLabel } from '@/shared/lib/overlap-label';
import { formatTon } from '@/shared/lib/ton-format';
import { ChannelAvatar, Popover, PulsingDot } from '@/shared/ui';
import { pressScale } from '@/shared/ui/animations';
import { ClockIcon, InfoIcon, TelegramIcon } from '@/shared/ui/icons';

type DealHeroSectionProps = {
  deal: Deal;
  statusConfig: StatusConfig;
  isTerminal: boolean;
  countdown: string | null;
};

export function DealHeroSection({ deal, statusConfig, isTerminal, countdown }: DealHeroSectionProps) {
  const { t, i18n } = useTranslation();
  const statusDescKey = `${statusConfig.i18nKey}Desc`;
  const telegramLink = deal.channelUsername ? `https://t.me/${deal.channelUsername}` : null;

  const overlap = buildOverlapLabel(deal.postFrequencyHours, deal.durationHours, t);
  const overlapLabel = overlap?.label ?? null;
  const hasOverlapTooltip = overlap?.hasTooltip ?? false;
  const freq = overlap?.freq;
  const dur = overlap?.dur;

  return (
    <div className="relative overflow-hidden am-deal-hero-gradient" data-color={statusConfig.color}>
      <div className="relative px-4 pt-6 pb-5">
        {/* Avatar + channel info */}
        <div className="flex items-center gap-3.5 mb-5">
          <motion.div {...(telegramLink ? pressScale : {})}>
            <ChannelAvatar
              title={deal.channelTitle}
              size="xl"
              badge={
                telegramLink ? (
                  <div className="w-[18px] h-[18px] rounded-full flex items-center justify-center border-2 border-bg-base bg-accent">
                    <TelegramIcon className="w-2.5 h-2.5 text-white" />
                  </div>
                ) : undefined
              }
              onClick={telegramLink ? () => window.open(telegramLink, '_blank') : undefined}
            />
          </motion.div>
          <div className="flex-1 min-w-0">
            <Text type="title2" weight="bold">
              <span className="am-truncate">{deal.channelTitle}</span>
            </Text>
            <Text type="subheadline2" color="secondary">
              {deal.channelUsername ? `@${deal.channelUsername}` : ''}
              {deal.channelUsername ? ' \u00b7 ' : ''}
              {t(`catalog.channel.postType.${deal.postType}`)}
            </Text>
          </div>
        </div>

        {/* Price */}
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1, duration: 0.3 }}
          className="text-center mb-4"
        >
          <Text type="title1" weight="bold">
            <span className="am-tabnum">{formatTon(deal.priceNano)}</span>
          </Text>
          <div className="w-12 h-[3px] rounded-sm bg-accent mx-auto mt-1.5 mb-1" />
          <div className="mt-0.5">
            <Text type="caption1" color="secondary">
              <span className="am-tabnum">{formatFiat(deal.priceNano)}</span>
            </Text>
          </div>
        </motion.div>

        {/* Chips row */}
        <div className="flex justify-center flex-wrap gap-2 mb-4">
          {overlapLabel &&
            (hasOverlapTooltip ? (
              <Popover
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
                <span className="am-hero-chip">
                  {overlapLabel}
                  <InfoIcon className="w-3 h-3 text-fg-tertiary" />
                </span>
              </Popover>
            ) : (
              <span className="am-hero-chip">{overlapLabel}</span>
            ))}
          <span className="am-hero-chip">{formatDate(deal.createdAt, i18n.language)}</span>
          {countdown && (
            <span className="am-hero-chip">
              <ClockIcon className="w-3.5 h-3.5 text-fg-secondary" />
              {countdown}
            </span>
          )}
        </div>

        {/* Deal message */}
        {deal.message && (
          <div className="am-deal-message mb-4">
            <Text type="caption1" color="secondary">
              &ldquo;{deal.message}&rdquo;
            </Text>
          </div>
        )}

        {/* Status pill */}
        <div className={`am-status-pill ${statusBgClass(statusConfig.color)}`}>
          {!isTerminal && <PulsingDot color={statusConfig.color} />}
          <div>
            <DealStatusBadge status={deal.status} />
            <div className="mt-1">
              <Text type="caption1" color="secondary">
                {t(statusDescKey)}
              </Text>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
