export type TonTransactionError =
  | { code: 'WALLET_NOT_CONNECTED' }
  | { code: 'USER_REJECTED' }
  | { code: 'INSUFFICIENT_FUNDS' }
  | { code: 'TIMEOUT' }
  | { code: 'NETWORK_MISMATCH' }
  | { code: 'DISCONNECTED_DURING_TX' }
  | { code: 'UNKNOWN'; message: string };

function extractMessage(error: unknown): string {
  if (error instanceof Error) return error.message;
  if (typeof error === 'string') return error;
  if (typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string') {
    return error.message;
  }
  return 'Unknown error';
}

export function mapTonConnectError(error: unknown): TonTransactionError {
  const message = extractMessage(error);
  const lower = message.toLowerCase();

  // MVP mapping: only the most common user-facing cases.
  if (
    lower.includes('reject') ||
    lower.includes('canceled') ||
    lower.includes('cancelled') ||
    lower.includes('cancel')
  ) {
    return { code: 'USER_REJECTED' };
  }
  if (lower.includes('timeout') || lower.includes('expired')) {
    return { code: 'TIMEOUT' };
  }

  return { code: 'UNKNOWN', message };
}

export function getErrorI18nKey(error: TonTransactionError): string {
  switch (error.code) {
    case 'WALLET_NOT_CONNECTED':
      return 'wallet.error.connectFirst';
    case 'USER_REJECTED':
      return 'wallet.error.walletRejected';
    case 'INSUFFICIENT_FUNDS':
      return 'wallet.error.insufficientTon';
    case 'TIMEOUT':
      return 'wallet.error.timeout';
    case 'NETWORK_MISMATCH':
      return 'wallet.error.networkMismatch';
    case 'DISCONNECTED_DURING_TX':
      return 'wallet.error.disconnected';
    case 'UNKNOWN':
      return 'wallet.error.transactionFailed';
  }
}
