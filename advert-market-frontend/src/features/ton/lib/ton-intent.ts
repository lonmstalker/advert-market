export type PendingTonIntent = {
  type: 'escrow_deposit';
  dealId: string;
  sentAt: number;
  address: string;
  amountNano: string;
};

const STORAGE_KEY = 'ton_pending_intent';
const TTL_MS = 30 * 60 * 1000; // 30 minutes

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function isPendingTonIntent(value: unknown): value is PendingTonIntent {
  if (!isRecord(value)) return false;
  return (
    value.type === 'escrow_deposit' &&
    typeof value.dealId === 'string' &&
    typeof value.sentAt === 'number' &&
    typeof value.address === 'string' &&
    typeof value.amountNano === 'string'
  );
}

export function savePendingIntent(intent: PendingTonIntent): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(intent));
}

export function loadPendingIntent(): PendingTonIntent | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) return null;

  try {
    const parsed: unknown = JSON.parse(raw);
    if (!isPendingTonIntent(parsed)) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }

    if (Date.now() - parsed.sentAt > TTL_MS) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }

    return parsed;
  } catch {
    sessionStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

export function clearPendingIntent(): void {
  sessionStorage.removeItem(STORAGE_KEY);
}
