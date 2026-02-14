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
import { copyToClipboard } from '@/shared/lib/clipboard';
import { formatDateTime } from '@/shared/lib/date-format';
import { formatFiat } from '@/shared/lib/fiat-format';
import { formatTon } from '@/shared/lib/ton-format';
import { BackButtonHandler, EmptyState, PageLoader } from '@/shared/ui';
import { fadeIn, pressScale } from '@/shared/ui/animations';
import { SadFaceIcon, TonDiamondIcon } from '@/shared/ui/icons';

export default function TransactionDetailPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { txId } = useParams<{ txId: string }>();

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

  return (
    <>
      <BackButtonHandler />
      <motion.div {...fadeIn} style={{ display: 'flex', flexDirection: 'column', minHeight: 'calc(100vh - 40px)' }}>
        {/* Hero section with gradient + diamonds */}
        <div style={{ position: 'relative', overflow: 'hidden' }}>
          {/* Gradient backdrop — semantic color based on direction */}
          <div
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              height: 140,
              background:
                tx.direction === 'income'
                  ? 'linear-gradient(180deg, rgba(52, 199, 89, 0.08) 0%, transparent 100%)'
                  : 'linear-gradient(180deg, rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.06) 0%, transparent 100%)',
              pointerEvents: 'none',
            }}
          />

          {/* Decorative TON diamonds */}
          <TonDiamondIcon
            style={{
              position: 'absolute',
              top: 16,
              right: 30,
              width: 14,
              height: 14,
              opacity: 0.2,
              color: 'var(--color-foreground-primary)',
              pointerEvents: 'none',
            }}
          />
          <TonDiamondIcon
            style={{
              position: 'absolute',
              top: 40,
              right: 68,
              width: 18,
              height: 18,
              opacity: 0.15,
              color: 'var(--color-foreground-primary)',
              pointerEvents: 'none',
            }}
          />
          <TonDiamondIcon
            style={{
              position: 'absolute',
              top: 24,
              left: 22,
              width: 12,
              height: 12,
              opacity: 0.22,
              color: 'var(--color-foreground-primary)',
              pointerEvents: 'none',
            }}
          />

          <div style={{ position: 'relative', textAlign: 'center', padding: '28px 16px 20px' }}>
            {/* Type icon in container */}
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: 16,
                background: getTransactionTypeTint(tx.type),
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 12px',
              }}
            >
              <TypeIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-secondary)' }} />
            </div>

            {/* Amount with animated entry */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1, duration: 0.35, ease: easeOut }}
            >
              <Text type="largeTitle" weight="bold">
                <span style={{ fontVariantNumeric: 'tabular-nums', color: amountColor }}>
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
              <Text type="caption1" color="secondary">
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
        <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 16 }}>
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

          {/* Blockchain details */}
          {(tx.txHash || tx.fromAddress || tx.toAddress) && (
            <Group>
              {tx.txHash && (
                <GroupItem
                  text={t('wallet.detail.txHash')}
                  onClick={() => copyToClipboard(tx.txHash as string)}
                  after={<span style={{ fontFamily: 'monospace', fontSize: 13 }}>{truncateAddress(tx.txHash)}</span>}
                />
              )}
              {tx.fromAddress && (
                <GroupItem
                  text={t('wallet.detail.from')}
                  onClick={() => copyToClipboard(tx.fromAddress as string)}
                  after={
                    <span style={{ fontFamily: 'monospace', fontSize: 13 }}>{truncateAddress(tx.fromAddress)}</span>
                  }
                />
              )}
              {tx.toAddress && (
                <GroupItem
                  text={t('wallet.detail.to')}
                  onClick={() => copyToClipboard(tx.toAddress as string)}
                  after={<span style={{ fontFamily: 'monospace', fontSize: 13 }}>{truncateAddress(tx.toAddress)}</span>}
                />
              )}
            </Group>
          )}

          {/* Explorer link */}
          {tx.txHash && (
            <motion.a
              {...pressScale}
              href={`https://tonviewer.com/transaction/${tx.txHash}`}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8,
                padding: '12px 16px',
                borderRadius: 12,
                background: 'var(--color-background-secondary)',
                border: '1px solid var(--color-border-separator)',
                textDecoration: 'none',
                cursor: 'pointer',
                marginBottom: 24,
              }}
            >
              <TonDiamondIcon style={{ width: 16, height: 16, color: 'var(--color-accent-primary)' }} />
              <Text type="body" weight="medium" color="accent">
                {t('wallet.detail.viewInExplorer')}
              </Text>
            </motion.a>
          )}
        </div>
      </motion.div>
    </>
  );
}
