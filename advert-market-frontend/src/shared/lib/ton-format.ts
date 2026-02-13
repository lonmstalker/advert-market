export const NANO_PER_TON = 1_000_000_000n;

function toBigInt(value: bigint | number | string): bigint {
  if (typeof value === 'bigint') return value;
  if (typeof value === 'number') {
    if (!Number.isInteger(value)) throw new Error(`Expected integer nanoTON, got float: ${value}`);
    return BigInt(value);
  }
  return BigInt(value);
}

export function formatTonCompact(nanoTon: bigint | number | string): string {
  const nano = toBigInt(nanoTon);
  const isNegative = nano < 0n;
  const abs = isNegative ? -nano : nano;

  const whole = abs / NANO_PER_TON;
  const frac = abs % NANO_PER_TON;

  let result: string;
  if (frac === 0n) {
    result = whole.toString();
  } else {
    const fracStr = frac.toString().padStart(9, '0').replace(/0+$/, '');
    result = `${whole}.${fracStr}`;
  }

  return isNegative ? `-${result}` : result;
}

export function formatTon(nanoTon: bigint | number | string): string {
  return `${formatTonCompact(nanoTon)} TON`;
}

const TON_FORMAT = /^-?\d+(\.\d{1,9})?$/;

export function computeCpm(priceNano: number, avgViews: number): number | null {
  if (avgViews <= 0) return null;
  return (priceNano / Number(NANO_PER_TON) / avgViews) * 1000;
}

export function formatCpm(cpm: number): string {
  if (cpm < 0.01) return '<0.01';
  if (cpm < 1) return cpm.toFixed(2);
  if (cpm < 10) return cpm.toFixed(1);
  return Math.round(cpm).toString();
}

export function parseTonToNano(ton: string): bigint {
  const trimmed = ton.trim();
  if (!trimmed) throw new Error('Empty input');
  if (!TON_FORMAT.test(trimmed)) throw new Error(`Invalid TON format: ${trimmed}`);

  const dotIndex = trimmed.indexOf('.');
  if (dotIndex === -1) {
    return BigInt(trimmed) * NANO_PER_TON;
  }

  const wholePart = trimmed.slice(0, dotIndex) || '0';
  const fracPart = trimmed
    .slice(dotIndex + 1)
    .slice(0, 9)
    .padEnd(9, '0');

  const isNegative = wholePart.startsWith('-');
  const absWhole = isNegative ? wholePart.slice(1) : wholePart;

  const result = BigInt(absWhole) * NANO_PER_TON + BigInt(fracPart);
  return isNegative ? -result : result;
}
