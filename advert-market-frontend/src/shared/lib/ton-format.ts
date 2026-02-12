export const NANO_PER_TON = 1_000_000_000n;

function toBigInt(value: bigint | number | string): bigint {
  if (typeof value === 'bigint') return value;
  if (typeof value === 'number') return BigInt(Math.trunc(value));
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

export function parseTonToNano(ton: string): bigint {
  const trimmed = ton.trim();
  if (!trimmed) throw new Error('Empty input');

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
