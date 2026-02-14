import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Transaction } from '../../types/wallet';
import { groupByDay } from '../day-group';

function makeTx(id: string, createdAt: string): Transaction {
  return {
    id,
    type: 'payout',
    status: 'confirmed',
    amountNano: '1000000000',
    direction: 'income',
    dealId: null,
    channelTitle: null,
    description: 'Test tx',
    createdAt,
  };
}

const mockT = ((key: string) => {
  const translations: Record<string, string> = {
    'wallet.dayGroup.today': 'Today',
    'wallet.dayGroup.yesterday': 'Yesterday',
  };
  return translations[key] ?? key;
}) as unknown as import('i18next').TFunction;

describe('groupByDay', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-02-14T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('groups transactions by date into DayGroup[]', () => {
    const txs = [makeTx('a', '2026-02-14T10:00:00Z'), makeTx('b', '2026-02-13T09:00:00Z')];
    const groups = groupByDay(txs, 'en', mockT);
    expect(groups).toHaveLength(2);
  });

  it('labels today with t(wallet.dayGroup.today)', () => {
    const txs = [makeTx('a', '2026-02-14T10:00:00Z')];
    const groups = groupByDay(txs, 'en', mockT);
    expect(groups[0].label).toBe('Today');
  });

  it('labels yesterday with t(wallet.dayGroup.yesterday)', () => {
    const txs = [makeTx('a', '2026-02-13T09:00:00Z')];
    const groups = groupByDay(txs, 'en', mockT);
    expect(groups[0].label).toBe('Yesterday');
  });

  it('labels older dates via Intl.DateTimeFormat(locale)', () => {
    const txs = [makeTx('a', '2026-01-15T12:00:00Z')];
    const groups = groupByDay(txs, 'en', mockT);
    expect(groups[0].label).not.toBe('Today');
    expect(groups[0].label).not.toBe('Yesterday');
    expect(groups[0].label).toContain('15');
  });

  it('preserves order within group', () => {
    const txs = [makeTx('first', '2026-02-14T10:00:00Z'), makeTx('second', '2026-02-14T08:00:00Z')];
    const groups = groupByDay(txs, 'en', mockT);
    expect(groups[0].transactions).toHaveLength(2);
    expect(groups[0].transactions[0].id).toBe('first');
    expect(groups[0].transactions[1].id).toBe('second');
  });

  it('returns [] for empty input', () => {
    expect(groupByDay([], 'en', mockT)).toEqual([]);
  });

  it('same date produces one group', () => {
    const txs = [makeTx('a', '2026-02-14T10:00:00Z'), makeTx('b', '2026-02-14T08:00:00Z')];
    const groups = groupByDay(txs, 'en', mockT);
    expect(groups).toHaveLength(1);
  });

  it('different dates produce separate groups', () => {
    const txs = [
      makeTx('a', '2026-02-14T10:00:00Z'),
      makeTx('b', '2026-02-13T09:00:00Z'),
      makeTx('c', '2026-01-15T12:00:00Z'),
    ];
    const groups = groupByDay(txs, 'en', mockT);
    expect(groups).toHaveLength(3);
  });
});
