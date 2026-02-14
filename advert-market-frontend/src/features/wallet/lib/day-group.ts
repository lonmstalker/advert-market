import type { TFunction } from 'i18next';
import type { Transaction } from '../types/wallet';

export type DayGroup = {
  date: string;
  label: string;
  transactions: Transaction[];
};

export function groupByDay(transactions: Transaction[], locale: string, t: TFunction): DayGroup[] {
  const groups = new Map<string, Transaction[]>();

  for (const tx of transactions) {
    const dateKey = tx.createdAt.slice(0, 10);
    const existing = groups.get(dateKey);
    if (existing) {
      existing.push(tx);
    } else {
      groups.set(dateKey, [tx]);
    }
  }

  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  const todayKey = formatDateKey(today);
  const yesterdayKey = formatDateKey(yesterday);

  const result: DayGroup[] = [];
  for (const [date, txs] of groups) {
    let label: string;
    if (date === todayKey) {
      label = t('wallet.dayGroup.today');
    } else if (date === yesterdayKey) {
      label = t('wallet.dayGroup.yesterday');
    } else {
      label = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long' }).format(new Date(date));
    }
    result.push({ date, label, transactions: txs });
  }

  return result;
}

function formatDateKey(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}
