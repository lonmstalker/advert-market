import { Button, Spinner, Text } from '@telegram-tools/ui-kit';
import { TonConnectButton } from '@tonconnect/ui-react';
import { motion } from 'motion/react';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useDepositPolling } from '@/features/ton/hooks/useDepositPolling';
import { useTonTransaction } from '@/features/ton/hooks/useTonTransaction';
import { useTonWalletStatus } from '@/features/ton/hooks/useTonWalletStatus';
import { getErrorI18nKey, type TonTransactionError } from '@/features/ton/lib/ton-errors';
import { clearPendingIntent, loadPendingIntent, savePendingIntent } from '@/features/ton/lib/ton-intent';
import { useToast } from '@/shared/hooks/use-toast';
import { formatTon } from '@/shared/lib/ton-format';
import { pressScale } from '@/shared/ui/animations';
import { usePaymentContext } from './PaymentContext';

function truncateAddress(addr: string): string {
  if (addr.length <= 16) return addr;
  return `${addr.slice(0, 8)}...${addr.slice(-6)}`;
}

function getDepositStatusLabelKey(status: string): string | null {
  switch (status) {
    case 'TX_DETECTED':
      return 'wallet.status.txDetected';
    case 'CONFIRMING':
      return 'wallet.status.confirming';
    case 'AWAITING_OPERATOR_REVIEW':
      return 'wallet.status.operatorReview';
    case 'OVERPAID':
      return 'wallet.status.overpaid';
    default:
      return null;
  }
}

export function PaymentSheetContent() {
  const { t } = useTranslation();
  const { showSuccess, showError, showInfo } = useToast();
  const { dealId, onClose } = usePaymentContext();

  const { isConnected, isConnectionRestored, friendlyAddress } = useTonWalletStatus();
  const { send, isPending: isTxPending } = useTonTransaction();

  const [pollingEnabled, setPollingEnabled] = useState(false);

  const { depositInfo, isLoading, isError, depositStatus, confirmations, requiredConfirmations, isPolling, refetch } =
    useDepositPolling(dealId, {
      enabled: pollingEnabled,
      onConfirmed: () => {
        showSuccess(t('wallet.toast.paymentConfirmed'));
        clearPendingIntent();
        onClose();
      },
      onTimeout: () => {
        showError(t('wallet.error.pollingTimeout'));
        clearPendingIntent();
        setPollingEnabled(false);
      },
    });

  useEffect(() => {
    const intent = loadPendingIntent();
    if (!intent) return;
    if (intent.dealId !== dealId) return;
    setPollingEnabled(true);
  }, [dealId]);

  const amountNano = depositInfo?.amountNano ?? null;
  const escrowAddress = depositInfo?.escrowAddress ?? null;

  const commissionLabel = t('deals.payment.platformCommission');

  const walletLine = useMemo(() => {
    if (!isConnectionRestored) return null;
    if (!isConnected || !friendlyAddress) return t('deals.payment.walletNotConnected');
    return truncateAddress(friendlyAddress);
  }, [friendlyAddress, isConnected, isConnectionRestored, t]);

  const canPay = isConnectionRestored && isConnected && !isTxPending && !!escrowAddress && !!amountNano;

  const handlePay = async () => {
    if (!escrowAddress || !amountNano) return;

    savePendingIntent({
      type: 'escrow_deposit',
      dealId,
      sentAt: Date.now(),
      address: escrowAddress,
      amountNano,
    });

    try {
      await send({ address: escrowAddress, amountNano });
      showSuccess(t('wallet.toast.paymentSent'));
      setPollingEnabled(true);
      refetch();
    } catch (err: unknown) {
      const txErr = err as TonTransactionError;
      showError(t(getErrorI18nKey(txErr)));
    }
  };

  const statusLabelKey = depositStatus ? getDepositStatusLabelKey(depositStatus) : null;
  const statusLabel =
    statusLabelKey === 'wallet.status.confirming'
      ? t(statusLabelKey, { current: confirmations ?? 0, required: requiredConfirmations ?? 0 })
      : statusLabelKey
        ? t(statusLabelKey)
        : null;

  useEffect(() => {
    if (!depositStatus) return;
    if (depositStatus !== 'UNDERPAID') return;
    showError(t('wallet.error.underpaid'));
  }, [depositStatus, showError, t]);

  useEffect(() => {
    if (!depositStatus) return;
    if (depositStatus !== 'EXPIRED') return;
    showError(t('wallet.error.depositExpired'));
    clearPendingIntent();
    setPollingEnabled(false);
  }, [depositStatus, showError, t]);

  useEffect(() => {
    if (!depositStatus) return;
    if (depositStatus !== 'REJECTED') return;
    showError(t('wallet.error.depositRejected'));
    clearPendingIntent();
    setPollingEnabled(false);
  }, [depositStatus, showError, t]);

  useEffect(() => {
    if (!depositStatus) return;
    if (depositStatus !== 'OVERPAID') return;
    showInfo(t('wallet.status.overpaid'));
  }, [depositStatus, showInfo, t]);

  return (
    <div data-testid="payment-sheet" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Text type="title2" weight="bold">
        {t('deals.payment.title')}
      </Text>

      {isLoading && (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 24 }}>
          <Spinner size="24px" color="accent" />
        </div>
      )}

      {isError && (
        <Text type="body" color="danger">
          {t('deals.payment.error')}
        </Text>
      )}

      {!isLoading && !isError && amountNano && (
        <>
          <div style={{ textAlign: 'center' }}>
            <Text type="hero" weight="bold">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(amountNano)}</span>
            </Text>
            <div style={{ marginTop: 6 }}>
              <Text type="caption1" color="secondary">
                {commissionLabel}
              </Text>
            </div>
          </div>

          <div style={{ padding: '10px 12px', borderRadius: 12, background: 'var(--color-background-secondary)' }}>
            <Text type="caption1" color="secondary">
              {t('deals.payment.wallet')}
            </Text>
            <div style={{ marginTop: 4 }}>
              <Text type="body" weight="medium">
                {walletLine ?? ''}
              </Text>
            </div>
          </div>

          {!isConnected && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'center' }}>
                <TonConnectButton />
              </div>
            </div>
          )}

          {statusLabel && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {isPolling && <Spinner size="16px" color="accent" />}
              <Text type="caption1" color="secondary">
                {statusLabel}
              </Text>
            </div>
          )}

          <Text type="caption1" color="secondary">
            {t('deals.payment.escrowNote')}
          </Text>

          <motion.div {...pressScale}>
            <Button
              text={t('deals.payment.pay')}
              type="primary"
              onClick={handlePay}
              disabled={!canPay}
              loading={isTxPending}
            />
          </motion.div>
        </>
      )}
    </div>
  );
}
