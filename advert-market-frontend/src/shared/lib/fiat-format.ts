import { NANO_PER_TON } from './ton-format';

const MOCK_RATES = {
  USD: { rate: 5.5, symbol: '$' },
  EUR: { rate: 5.0, symbol: '\u20AC' },
  RUB: { rate: 500, symbol: '\u20BD' },
} as const;

export function formatFiat(nanoTon: number | bigint | string, currency = 'USD'): string {
  const rates = MOCK_RATES as Record<string, { rate: number; symbol: string } | undefined>;
  const { rate, symbol } = rates[currency] ?? MOCK_RATES.USD;
  const nano =
    typeof nanoTon === 'bigint' ? nanoTon : typeof nanoTon === 'number' ? BigInt(Math.round(nanoTon)) : BigInt(nanoTon);
  const tonValue = Number(nano) / Number(NANO_PER_TON);
  const fiat = tonValue * rate;
  return `~${symbol}${fiat.toFixed(2)}`;
}
