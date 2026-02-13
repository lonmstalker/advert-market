import type { DealRole, DealStatus } from '../types/deal';

export type DealActionType =
  | 'accept'
  | 'reject'
  | 'cancel'
  | 'counter_offer'
  | 'reply'
  | 'pay'
  | 'approve_creative'
  | 'request_revision'
  | 'publish'
  | 'schedule';

export type DealAction = {
  type: DealActionType;
  i18nKey: string;
  variant: 'primary' | 'secondary' | 'destructive';
  requiresConfirm: boolean;
};

const ACTION_DEFS: Record<DealActionType, Omit<DealAction, 'type'>> = {
  accept: { i18nKey: 'deals.actions.accept', variant: 'primary', requiresConfirm: false },
  reject: { i18nKey: 'deals.actions.reject', variant: 'destructive', requiresConfirm: true },
  cancel: { i18nKey: 'deals.actions.cancel', variant: 'destructive', requiresConfirm: true },
  counter_offer: { i18nKey: 'deals.actions.counterOffer', variant: 'secondary', requiresConfirm: false },
  reply: { i18nKey: 'deals.actions.reply', variant: 'primary', requiresConfirm: false },
  pay: { i18nKey: 'deals.actions.pay', variant: 'primary', requiresConfirm: false },
  approve_creative: { i18nKey: 'deals.actions.approveCreative', variant: 'primary', requiresConfirm: false },
  request_revision: { i18nKey: 'deals.actions.requestRevision', variant: 'secondary', requiresConfirm: false },
  publish: { i18nKey: 'deals.actions.publish', variant: 'primary', requiresConfirm: false },
  schedule: { i18nKey: 'deals.actions.schedule', variant: 'secondary', requiresConfirm: false },
};

type ActionMatrix = Record<string, { ADVERTISER?: DealActionType[]; OWNER?: DealActionType[] }>;

const MATRIX: ActionMatrix = {
  OFFER_PENDING: {
    ADVERTISER: ['cancel'],
    OWNER: ['accept', 'counter_offer', 'reject'],
  },
  NEGOTIATING: {
    ADVERTISER: ['reply', 'cancel'],
    OWNER: ['reply', 'reject'],
  },
  AWAITING_PAYMENT: {
    ADVERTISER: ['pay'],
  },
  CREATIVE_SUBMITTED: {
    ADVERTISER: ['approve_creative', 'request_revision'],
  },
  CREATIVE_APPROVED: {
    OWNER: ['publish', 'schedule'],
  },
};

export function getDealActions(status: DealStatus, role: DealRole): DealAction[] {
  const entry = MATRIX[status];
  if (!entry) return [];

  const types = entry[role];
  if (!types) return [];

  return types.map((type) => ({ type, ...ACTION_DEFS[type] }));
}
