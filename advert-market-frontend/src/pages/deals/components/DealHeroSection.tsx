import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import type { CSSProperties } from 'react';
import { useTranslation } from 'react-i18next';
import { DealStatusBadge } from '@/features/deals/components/DealStatusBadge';
import type { StatusConfig } from '@/features/deals/lib/deal-status';
import { statusBgVar } from '@/features/deals/lib/deal-status';
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

const heroChipStyle: CSSProperties = {
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
    <div style={{ position: 'relative', overflow: 'hidden' }}>
      <div
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: 160,
          background: `linear-gradient(180deg, ${statusBgVar(statusConfig.color)} 0%, transparent 100%)`,
          pointerEvents: 'none',
        }}
      />

      <div style={{ position: 'relative', padding: '20px 16px 16px' }}>
        {/* Avatar + channel info */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
          <motion.div {...(telegramLink ? pressScale : {})}>
            <ChannelAvatar
              title={deal.channelTitle}
              size="xl"
              badge={
                telegramLink ? (
                  <div
                    style={{
                      width: 18,
                      height: 18,
                      borderRadius: '50%',
                      background: 'var(--color-link)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      border: '2px solid var(--color-background-base)',
                    }}
                  >
                    <TelegramIcon style={{ width: 10, height: 10, color: 'var(--color-static-white)' }} />
                  </div>
                ) : undefined
              }
              onClick={telegramLink ? () => window.open(telegramLink, '_blank') : undefined}
            />
          </motion.div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <Text type="title2" weight="bold">
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                {deal.channelTitle}
              </span>
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
          style={{ textAlign: 'center', marginBottom: 14 }}
        >
          <Text type="title1" weight="bold">
            <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(deal.priceNano)}</span>
          </Text>
          <div
            style={{
              width: 48,
              height: 3,
              borderRadius: 2,
              background: 'var(--color-accent-primary)',
              margin: '6px auto 4px',
            }}
          />
          <div style={{ marginTop: 2 }}>
            <Text type="caption1" color="secondary">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(deal.priceNano)}</span>
            </Text>
          </div>
        </motion.div>

        {/* Chips row */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'center',
            flexWrap: 'wrap',
            gap: 8,
            marginBottom: 14,
          }}
        >
          {overlapLabel &&
            (hasOverlapTooltip ? (
              <Popover
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
                <span style={heroChipStyle}>
                  {overlapLabel}
                  <InfoIcon style={{ width: 12, height: 12, color: 'var(--color-foreground-tertiary)' }} />
                </span>
              </Popover>
            ) : (
              <span style={heroChipStyle}>{overlapLabel}</span>
            ))}
          <span style={heroChipStyle}>{formatDate(deal.createdAt, i18n.language)}</span>
          {countdown && (
            <span style={heroChipStyle}>
              <ClockIcon style={{ width: 14, height: 14, color: 'var(--color-foreground-secondary)' }} />
              {countdown}
            </span>
          )}
        </div>

        {/* Deal message */}
        {deal.message && (
          <div
            style={{
              background: 'var(--color-background-secondary)',
              borderRadius: 10,
              padding: '10px 14px',
              marginBottom: 14,
            }}
          >
            <div style={{ whiteSpace: 'pre-wrap', fontStyle: 'italic', textAlign: 'center' }}>
              <Text type="caption1" color="secondary">
                &ldquo;{deal.message}&rdquo;
              </Text>
            </div>
          </div>
        )}

        {/* Status pill */}
        <div
          style={{
            padding: '10px 14px',
            borderRadius: 10,
            background: statusBgVar(statusConfig.color),
            display: 'flex',
            alignItems: 'center',
            gap: 8,
          }}
        >
          {!isTerminal && <PulsingDot color={statusConfig.color} />}
          <div>
            <DealStatusBadge status={deal.status} />
            <div style={{ marginTop: 4 }}>
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
