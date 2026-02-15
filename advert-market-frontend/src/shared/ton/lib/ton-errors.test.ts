import { describe, expect, it } from 'vitest';
import { getErrorI18nKey, mapTonConnectError } from './ton-errors';

describe('ton-errors', () => {
  it('maps user rejection', () => {
    const err = mapTonConnectError(new Error('User rejected'));
    expect(err.code).toBe('USER_REJECTED');
    expect(getErrorI18nKey(err)).toBe('wallet.error.walletRejected');
  });

  it('maps timeout', () => {
    const err = mapTonConnectError(new Error('Request timeout'));
    expect(err.code).toBe('TIMEOUT');
    expect(getErrorI18nKey(err)).toBe('wallet.error.timeout');
  });

  it('maps unknown errors', () => {
    const err = mapTonConnectError(new Error('Boom'));
    expect(err.code).toBe('UNKNOWN');
    expect(getErrorI18nKey(err)).toBe('wallet.error.transactionFailed');
  });
});
