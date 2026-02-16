export { fetchDeal, fetchDealDeposit, fetchDeals, transitionDeal } from './api/deals';
export { DealActions } from './components/DealActions';
export { DealInfoCard } from './components/DealInfoCard';
export { DealListItem } from './components/DealListItem';
export { DealStatusBadge } from './components/DealStatusBadge';
export { DealTimeline } from './components/DealTimeline';
export { NegotiateProvider } from './components/NegotiateContext';
export { NegotiateSheetContent } from './components/NegotiateSheet';
export { useDealDetail } from './hooks/useDealDetail';
export { useDealTransition } from './hooks/useDealTransition';
export type { DealAction, DealActionType } from './lib/deal-actions';
export { getDealActions } from './lib/deal-actions';
export { mapDealDetailDtoToViewModel, mapDealDtoToViewModel } from './lib/deal-mapper';
export type { StatusColor, StatusConfig, TimelineStep } from './lib/deal-status';
export { buildTimelineSteps, getPollingInterval, getStatusConfig } from './lib/deal-status';
export type {
  Deal,
  DealChannelMetadata,
  DealDepositInfo,
  DealDetailDto,
  DealDto,
  DealEvent,
  DealEventDto,
  DealListItem as DealListItemType,
  DealRole,
  DealStatus,
  DealTimeline as DealTimelineType,
  DealTransitionResponse,
  TransitionRequest,
} from './types/deal';
