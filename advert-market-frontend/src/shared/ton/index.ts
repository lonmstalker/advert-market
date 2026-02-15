export { fetchDealDepositInfo } from './api/deposit';
export type { DepositPollingOptions } from './hooks/useDepositPolling';
export { useDepositPolling } from './hooks/useDepositPolling';
export type { TonTransactionParams } from './hooks/useTonTransaction';
export { useTonTransaction } from './hooks/useTonTransaction';
export { useTonWalletStatus } from './hooks/useTonWalletStatus';
export type { TonTransactionError } from './lib/ton-errors';
export { getErrorI18nKey, mapTonConnectError } from './lib/ton-errors';

export type { PendingTonIntent } from './lib/ton-intent';
export { clearPendingIntent, loadPendingIntent, savePendingIntent } from './lib/ton-intent';
export type { DepositInfo } from './types/ton';
export { DepositInfoSchema } from './types/ton';
