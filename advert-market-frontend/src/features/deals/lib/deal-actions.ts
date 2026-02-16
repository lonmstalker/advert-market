import type { DealRole, DealStatus } from '../types/deal';

export type DealActionType =
  | 'accept'
  | 'reject'
  | 'cancel'
  | 'counter_offer'
  | 'pay'
  | 'approve_creative'
  | 'request_revision'
  | 'dispute'
  | 'publish'
  | 'schedule';

export type DealAction = {
  type: DealActionType;
  i18nKey: string;
  variant: 'primary' | 'secondary' | 'destructive';
  requiresConfirm: boolean;
  targetStatus?: DealStatus;
  requiresReason?: boolean;
};

const ACTION_DEFS: Record<DealActionType, Omit<DealAction, 'type'>> = {
  accept: {
    i18nKey: 'deals.actions.accept',
    variant: 'primary',
    requiresConfirm: false,
    targetStatus: 'ACCEPTED',
  },
  reject: {
    i18nKey: 'deals.actions.reject',
    variant: 'destructive',
    requiresConfirm: true,
    targetStatus: 'CANCELLED',
  },
  cancel: {
    i18nKey: 'deals.actions.cancel',
    variant: 'destructive',
    requiresConfirm: true,
    targetStatus: 'CANCELLED',
  },
  counter_offer: {
    i18nKey: 'deals.actions.counterOffer',
    variant: 'secondary',
    requiresConfirm: false,
    targetStatus: 'NEGOTIATING',
    requiresReason: true,
  },
  pay: {
    i18nKey: 'deals.actions.pay',
    variant: 'primary',
    requiresConfirm: false,
  },
  approve_creative: {
    i18nKey: 'deals.actions.approveCreative',
    variant: 'primary',
    requiresConfirm: false,
    targetStatus: 'CREATIVE_APPROVED',
  },
  request_revision: {
    i18nKey: 'deals.actions.requestRevision',
    variant: 'secondary',
    requiresConfirm: false,
    targetStatus: 'FUNDED',
    requiresReason: true,
  },
  dispute: {
    i18nKey: 'deals.actions.dispute',
    variant: 'secondary',
    requiresConfirm: true,
    targetStatus: 'DISPUTED',
    requiresReason: true,
  },
  publish: {
    i18nKey: 'deals.actions.publish',
    variant: 'primary',
    requiresConfirm: false,
    targetStatus: 'PUBLISHED',
  },
  schedule: {
    i18nKey: 'deals.actions.schedule',
    variant: 'secondary',
    requiresConfirm: false,
    targetStatus: 'SCHEDULED',
  },
};

type ActionMatrix = Record<string, { ADVERTISER?: DealActionType[]; OWNER?: DealActionType[] }>;

const MATRIX: ActionMatrix = {
  OFFER_PENDING: {
    ADVERTISER: ['cancel'],
    OWNER: ['accept', 'counter_offer', 'reject'],
  },
  NEGOTIATING: {
    ADVERTISER: ['cancel'],
    OWNER: ['accept', 'counter_offer', 'reject'],
  },
  ACCEPTED: {
    ADVERTISER: ['cancel'],
    OWNER: ['cancel'],
  },
  AWAITING_PAYMENT: {
    ADVERTISER: ['pay', 'cancel'],
  },
  FUNDED: {
    ADVERTISER: ['cancel'],
    OWNER: ['cancel'],
  },
  SCHEDULED: {
    ADVERTISER: ['cancel'],
    OWNER: ['cancel'],
  },
};

const CREATIVE_MATRIX: ActionMatrix = {
  CREATIVE_SUBMITTED: {
    ADVERTISER: ['approve_creative', 'request_revision', 'dispute'],
  },
  CREATIVE_APPROVED: {
    ADVERTISER: ['cancel'],
    OWNER: ['publish', 'schedule', 'cancel'],
  },
};

export function getDealActions(
  status: DealStatus,
  role: DealRole,
  options?: { creativeFlowEnabled?: boolean },
): DealAction[] {
  const creativeFlowEnabled = options?.creativeFlowEnabled ?? false;

  const entry = creativeFlowEnabled
    ? (MATRIX[status] ?? CREATIVE_MATRIX[status])
    : MATRIX[status];

  if (!entry) return [];

  const types = entry[role];
  if (!types) return [];

  return types.map((type) => ({ type, ...ACTION_DEFS[type] }));
}
