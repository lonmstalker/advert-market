import { NANO_PER_TON } from './ton-format';

const MOCK_TON_USD = 5.5;

export function formatFiat(nanoTon: number | bigint, rate = MOCK_TON_USD): string {
  const nano = typeof nanoTon === 'bigint' ? nanoTon : BigInt(Math.round(nanoTon));
  const tonValue = Number(nano) / Number(NANO_PER_TON);
  const usd = tonValue * rate;
  return `~$${usd.toFixed(2)}`;
}
