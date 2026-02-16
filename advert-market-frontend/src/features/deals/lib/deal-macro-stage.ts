import type { DealStatus } from '../types/deal';

export type MacroStage = 'negotiation' | 'payment' | 'publication' | 'terminal';

const MACRO_STAGE_MAP = {
  DRAFT: 'negotiation',
  OFFER_PENDING: 'negotiation',
  NEGOTIATING: 'negotiation',
  ACCEPTED: 'negotiation',
  AWAITING_PAYMENT: 'payment',
  FUNDED: 'payment',
  CREATIVE_SUBMITTED: 'payment',
  CREATIVE_APPROVED: 'payment',
  SCHEDULED: 'publication',
  PUBLISHED: 'publication',
  DELIVERY_VERIFYING: 'publication',
  COMPLETED_RELEASED: 'terminal',
  DISPUTED: 'terminal',
  CANCELLED: 'terminal',
  EXPIRED: 'terminal',
  REFUNDED: 'terminal',
  PARTIALLY_REFUNDED: 'terminal',
} as const satisfies Record<DealStatus, MacroStage>;

export function getMacroStage(status: DealStatus): MacroStage {
  return MACRO_STAGE_MAP[status];
}

export type MiniTimelineStep = {
  stage: MacroStage;
  state: 'completed' | 'active' | 'pending';
  i18nKey: string;
};

const VISIBLE_STAGES: readonly Exclude<MacroStage, 'terminal'>[] = ['negotiation', 'payment', 'publication'] as const;

const STAGE_I18N: Record<Exclude<MacroStage, 'terminal'>, string> = {
  negotiation: 'deals.macro.negotiation',
  payment: 'deals.macro.payment',
  publication: 'deals.macro.publication',
};

export function getTimelineConfig(currentStatus: DealStatus): MiniTimelineStep[] {
  const macro = getMacroStage(currentStatus);
  const isTerminal = macro === 'terminal';

  const activeIndex = isTerminal ? VISIBLE_STAGES.length : VISIBLE_STAGES.indexOf(macro);

  return VISIBLE_STAGES.map((stage, i) => ({
    stage,
    state: i < activeIndex ? 'completed' : i === activeIndex ? 'active' : 'pending',
    i18nKey: STAGE_I18N[stage],
  }));
}
