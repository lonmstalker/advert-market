export { fetchDeal, fetchDeals, fetchDealTimeline, negotiateDeal, transitionDeal } from './api/deals';
export { DealActions } from './components/DealActions';
export { DealInfoCard } from './components/DealInfoCard';
export { DealListItem } from './components/DealListItem';
export { DealStatusBadge } from './components/DealStatusBadge';
export { DealTimeline } from './components/DealTimeline';
export { NegotiateSheetContent, setNegotiateSheetProps } from './components/NegotiateSheet';
export { useDealDetail } from './hooks/useDealDetail';
export { useDealTransition } from './hooks/useDealTransition';
export { buildTimelineSteps, getPollingInterval, getStatusConfig } from './lib/deal-status';
export { getDealActions } from './lib/deal-actions';

export type {
  Deal,
  DealEvent,
  DealListItem as DealListItemType,
  DealRole,
  DealStatus,
  DealTimeline as DealTimelineType,
  NegotiateRequest,
  TransitionRequest,
} from './types/deal';
export type { DealAction, DealActionType } from './lib/deal-actions';
export type { StatusColor, StatusConfig, TimelineStep } from './lib/deal-status';
