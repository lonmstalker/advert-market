import { describe, expect, it, vi } from 'vitest';
import { clearPendingIntent, loadPendingIntent, savePendingIntent } from './ton-intent';

describe('ton-intent', () => {
  it('saves and loads pending intent', () => {
    const now = Date.now();
    vi.spyOn(Date, 'now').mockReturnValue(now);

    savePendingIntent({
      type: 'escrow_deposit',
      dealId: 'deal-123',
      sentAt: now,
      address: 'UQ_TEST',
      amountNano: '1000',
    });

    expect(loadPendingIntent()).toEqual({
      type: 'escrow_deposit',
      dealId: 'deal-123',
      sentAt: now,
      address: 'UQ_TEST',
      amountNano: '1000',
    });
  });

  it('clears pending intent', () => {
    savePendingIntent({
      type: 'escrow_deposit',
      dealId: 'deal-123',
      sentAt: Date.now(),
      address: 'UQ_TEST',
      amountNano: '1000',
    });

    clearPendingIntent();
    expect(loadPendingIntent()).toBeNull();
  });

  it('returns null and clears storage when intent is expired', () => {
    const now = Date.now();
    vi.spyOn(Date, 'now').mockReturnValue(now);

    // TTL: 30 minutes (1800s) + 1ms
    const sentAt = now - 30 * 60 * 1000 - 1;

    savePendingIntent({
      type: 'escrow_deposit',
      dealId: 'deal-123',
      sentAt,
      address: 'UQ_TEST',
      amountNano: '1000',
    });

    expect(loadPendingIntent()).toBeNull();
    expect(sessionStorage.getItem('ton_pending_intent')).toBeNull();
  });
});
