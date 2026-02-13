import type { DealEvent, DealStatus } from '../types/deal';

export type StatusColor = 'accent' | 'warning' | 'success' | 'destructive' | 'secondary';

export type StatusConfig = {
  color: StatusColor;
  i18nKey: string;
};

const STATUS_CONFIG: Record<DealStatus, StatusConfig> = {
  DRAFT: { color: 'accent', i18nKey: 'deals.status.draft' },
  OFFER_PENDING: { color: 'accent', i18nKey: 'deals.status.offerPending' },
  NEGOTIATING: { color: 'accent', i18nKey: 'deals.status.negotiating' },
  ACCEPTED: { color: 'accent', i18nKey: 'deals.status.accepted' },
  AWAITING_PAYMENT: { color: 'warning', i18nKey: 'deals.status.awaitingPayment' },
  FUNDED: { color: 'accent', i18nKey: 'deals.status.funded' },
  CREATIVE_SUBMITTED: { color: 'accent', i18nKey: 'deals.status.creativeSubmitted' },
  CREATIVE_APPROVED: { color: 'accent', i18nKey: 'deals.status.creativeApproved' },
  SCHEDULED: { color: 'warning', i18nKey: 'deals.status.scheduled' },
  PUBLISHED: { color: 'warning', i18nKey: 'deals.status.published' },
  DELIVERY_VERIFYING: { color: 'warning', i18nKey: 'deals.status.deliveryVerifying' },
  COMPLETED_RELEASED: { color: 'success', i18nKey: 'deals.status.completedReleased' },
  DISPUTED: { color: 'destructive', i18nKey: 'deals.status.disputed' },
  CANCELLED: { color: 'secondary', i18nKey: 'deals.status.cancelled' },
  EXPIRED: { color: 'secondary', i18nKey: 'deals.status.expired' },
  REFUNDED: { color: 'secondary', i18nKey: 'deals.status.refunded' },
  PARTIALLY_REFUNDED: { color: 'secondary', i18nKey: 'deals.status.partiallyRefunded' },
};

export function getStatusConfig(status: DealStatus): StatusConfig {
  return STATUS_CONFIG[status];
}

const HAPPY_PATH: DealStatus[] = [
  'DRAFT',
  'OFFER_PENDING',
  'NEGOTIATING',
  'ACCEPTED',
  'AWAITING_PAYMENT',
  'FUNDED',
  'CREATIVE_SUBMITTED',
  'CREATIVE_APPROVED',
  'SCHEDULED',
  'PUBLISHED',
  'DELIVERY_VERIFYING',
  'COMPLETED_RELEASED',
];

const TERMINAL_STATUSES = new Set<DealStatus>([
  'CANCELLED',
  'EXPIRED',
  'REFUNDED',
  'PARTIALLY_REFUNDED',
  'DISPUTED',
  'COMPLETED_RELEASED',
]);

export type TimelineStep = {
  status: DealStatus;
  state: 'completed' | 'active' | 'pending';
  label: string;
  timestamp?: string;
  description?: string;
};

export function buildTimelineSteps(
  events: DealEvent[],
  currentStatus: DealStatus,
  t: (key: string) => string,
): TimelineStep[] {
  const completedStatuses = new Map<DealStatus, string>();
  for (const event of events) {
    if (!completedStatuses.has(event.status)) {
      completedStatuses.set(event.status, event.createdAt);
    }
  }

  const isTerminal = TERMINAL_STATUSES.has(currentStatus);
  const isCompleted = currentStatus === 'COMPLETED_RELEASED';

  const currentIndex = HAPPY_PATH.indexOf(currentStatus);

  const steps: TimelineStep[] = [];

  if (isTerminal && !isCompleted) {
    // Show completed events + current terminal status
    for (const status of HAPPY_PATH) {
      const timestamp = completedStatuses.get(status);
      if (timestamp) {
        steps.push({
          status,
          state: 'completed',
          label: t(STATUS_CONFIG[status].i18nKey),
          timestamp,
        });
      }
    }
    steps.push({
      status: currentStatus,
      state: 'active',
      label: t(STATUS_CONFIG[currentStatus].i18nKey),
      description: t(`deals.status.${statusToDescKey(currentStatus)}Desc`),
    });
  } else {
    // Happy path: completed + active + pending
    for (let i = 0; i < HAPPY_PATH.length; i++) {
      const status = HAPPY_PATH[i];
      const timestamp = completedStatuses.get(status);

      if (i < currentIndex) {
        steps.push({
          status,
          state: 'completed',
          label: t(STATUS_CONFIG[status].i18nKey),
          timestamp,
        });
      } else if (i === currentIndex) {
        steps.push({
          status,
          state: isCompleted ? 'completed' : 'active',
          label: t(STATUS_CONFIG[status].i18nKey),
          timestamp: isCompleted ? timestamp : undefined,
          description: isCompleted ? undefined : t(`deals.status.${statusToDescKey(status)}Desc`),
        });
      } else if (!isCompleted) {
        steps.push({
          status,
          state: 'pending',
          label: t(STATUS_CONFIG[status].i18nKey),
        });
      }
    }
  }

  return steps;
}

function statusToDescKey(status: DealStatus): string {
  return status.toLowerCase().replace(/_([a-z])/g, (_, c: string) => c.toUpperCase());
}

export function getPollingInterval(status: DealStatus): number | false {
  switch (status) {
    case 'AWAITING_PAYMENT':
    case 'DELIVERY_VERIFYING':
      return 10_000;
    case 'PUBLISHED':
      return 30_000;
    default:
      return false;
  }
}

const COLOR_VAR_MAP: Record<StatusColor, string> = {
  accent: 'var(--color-accent-primary)',
  warning: 'var(--color-warning)',
  success: 'var(--color-success)',
  destructive: 'var(--color-destructive)',
  secondary: 'var(--color-foreground-secondary)',
};

const COLOR_BG_MAP: Record<StatusColor, string> = {
  accent: 'rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.1)',
  warning: 'rgba(255, 159, 10, 0.1)',
  success: 'rgba(52, 199, 89, 0.1)',
  destructive: 'rgba(255, 59, 48, 0.1)',
  secondary: 'rgba(142, 142, 147, 0.1)',
};

export function statusColorVar(color: StatusColor): string {
  return COLOR_VAR_MAP[color];
}

export function statusBgVar(color: StatusColor): string {
  return COLOR_BG_MAP[color];
}
