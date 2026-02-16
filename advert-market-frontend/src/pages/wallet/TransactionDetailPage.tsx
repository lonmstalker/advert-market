import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { easeOut } from 'motion';
import { motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { TransactionStatusBadge } from '@/features/wallet/components/TransactionStatusBadge';
import { useTransactionDetail } from '@/features/wallet/hooks/useTransactionDetail';
import {
  getAmountColor,
  getTransactionTypeConfig,
  getTransactionTypeTint,
} from '@/features/wallet/lib/transaction-type';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { copyToClipboard } from '@/shared/lib/clipboard';
import { formatDateTime } from '@/shared/lib/date-format';
import { formatFiat } from '@/shared/lib/fiat-format';
import { formatTon } from '@/shared/lib/ton-format';
import { BackButtonHandler, EmptyState, PageLoader } from '@/shared/ui';
import { fadeIn } from '@/shared/ui/animations';
import { SadFaceIcon, TonDiamondIcon } from '@/shared/ui/icons';

export default function TransactionDetailPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { txId } = useParams<{ txId: string }>();
  const haptic = useHaptic();

  const { data: tx, isLoading, isError } = useTransactionDetail(txId as string);

  if (isLoading) {
    return (
      <>
        <BackButtonHandler />
        <PageLoader />
      </>
    );
  }

  if (isError || !tx) {
    return (
      <>
        <BackButtonHandler />
        <EmptyState
          icon={<SadFaceIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
          title={t('errors.notFound')}
          description={t('wallet.empty.description')}
          actionLabel={t('common.back')}
          onAction={() => navigate(-1)}
        />
      </>
    );
  }

  const config = getTransactionTypeConfig(tx.type);
  const amountColor = getAmountColor(tx.type, tx.direction);
  const sign = tx.direction === 'income' ? '+' : '\u2212';
  const TypeIcon = config.Icon;

  const truncateAddress = (addr: string) => `${addr.slice(0, 8)}...${addr.slice(-6)}`;

  const handleCopy = (text: string) => {
    haptic.impactOccurred('light');
    copyToClipboard(text);
  };

  return (
    <>
      <BackButtonHandler />
      <motion.div {...fadeIn} className="am-finance-page">
        <div className="am-finance-stack">
          <div
            className="am-finance-card"
            style={{
              overflow: 'hidden',
              position: 'relative',
              background: 'color-mix(in srgb, var(--am-card-surface) 92%, transparent)',
            }}
          >
            {/* Gradient backdrop — contained inside card */}
            <div
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                height: 110,
                background:
                  tx.direction === 'income' ? 'var(--am-hero-gradient-success)' : 'var(--am-hero-gradient-accent)',
                pointerEvents: 'none',
              }}
            />

            <div style={{ position: 'relative', textAlign: 'center', padding: '28px 16px 20px' }}>
              {/* Type icon — 56px */}
              <div
                style={{
                  width: 56,
                  height: 56,
                  borderRadius: '50%',
                  background: getTransactionTypeTint(tx.type),
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 12px',
                }}
              >
                <TypeIcon style={{ width: 30, height: 30, color: 'var(--color-foreground-secondary)' }} />
              </div>

              {/* Amount */}
              <motion.div
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1, duration: 0.35, ease: easeOut }}
              >
                <Text type="largeTitle" weight="bold">
                  <span className="am-wallet-headerAmount" style={{ color: amountColor }}>
                    {sign}
                    {formatTon(tx.amountNano)}
                  </span>
                </Text>
              </motion.div>

              {/* Fiat equivalent */}
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.25, duration: 0.3 }}
                style={{ marginTop: 4 }}
              >
                <Text type="subheadline2" color="secondary">
                  <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(tx.amountNano)}</span>
                </Text>
              </motion.div>

              {/* Status badge */}
              <div style={{ marginTop: 12 }}>
                <TransactionStatusBadge status={tx.status} />
              </div>
            </div>
          </div>

          {/* Details */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Group>
              <GroupItem text={t('wallet.detail.type')} after={<Text type="body">{t(config.i18nKey)}</Text>} />
              <GroupItem
                text={t('wallet.detail.date')}
                after={<Text type="body">{formatDateTime(tx.createdAt, i18n.language)}</Text>}
              />
              {tx.description && (
                <GroupItem text={t('wallet.detail.description')} after={<Text type="body">{tx.description}</Text>} />
              )}
              {tx.channelTitle && (
                <GroupItem text={t('wallet.detail.channel')} after={<Text type="body">{tx.channelTitle}</Text>} />
              )}
              {tx.dealId && (
                <GroupItem
                  text={t('wallet.detail.deal')}
                  onClick={() => navigate(`/deals/${tx.dealId}`)}
                  after={
                    <Text type="body" color="accent">
                      {t('wallet.detail.viewDeal')} →
                    </Text>
                  }
                />
              )}
              {tx.commissionNano && (
                <GroupItem
                  text={t('wallet.detail.commission')}
                  after={
                    <Text type="body">
                      <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(tx.commissionNano)}</span>
                    </Text>
                  }
                />
              )}
            </Group>

            {/* Blockchain details — using Text caption2 with monospace */}
            {(tx.txHash || tx.fromAddress || tx.toAddress) && (
              <Group>
                {tx.txHash && (
                  <GroupItem
                    text={t('wallet.detail.txHash')}
                    onClick={() => handleCopy(tx.txHash as string)}
                    after={
                      <Text type="caption2">
                        <span style={{ fontFamily: 'monospace' }}>{truncateAddress(tx.txHash)}</span>
                      </Text>
                    }
                  />
                )}
                {tx.fromAddress && (
                  <GroupItem
                    text={t('wallet.detail.from')}
                    onClick={() => handleCopy(tx.fromAddress as string)}
                    after={
                      <Text type="caption2">
                        <span style={{ fontFamily: 'monospace' }}>{truncateAddress(tx.fromAddress)}</span>
                      </Text>
                    }
                  />
                )}
                {tx.toAddress && (
                  <GroupItem
                    text={t('wallet.detail.to')}
                    onClick={() => handleCopy(tx.toAddress as string)}
                    after={
                      <Text type="caption2">
                        <span style={{ fontFamily: 'monospace' }}>{truncateAddress(tx.toAddress)}</span>
                      </Text>
                    }
                  />
                )}
              </Group>
            )}

            {/* Explorer link */}
            {tx.txHash && (
              <div>
                <Group>
                  <GroupItem
                    before={
                      <div
                        style={{
                          width: 32,
                          height: 32,
                          borderRadius: '50%',
                          background: 'color-mix(in srgb, var(--am-soft-accent-bg) 82%, transparent)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        <TonDiamondIcon style={{ width: 16, height: 16, color: 'var(--color-accent-primary)' }} />
                      </div>
                    }
                    text={
                      <Text type="body" weight="medium" color="accent">
                        {t('wallet.detail.viewInExplorer')}
                      </Text>
                    }
                    chevron
                    onClick={() => {
                      haptic.impactOccurred('light');
                      window.open(`https://tonviewer.com/transaction/${tx.txHash}`, '_blank', 'noopener,noreferrer');
                    }}
                  />
                </Group>
              </div>
            )}
          </div>
        </div>
      </motion.div>
    </>
  );
}
