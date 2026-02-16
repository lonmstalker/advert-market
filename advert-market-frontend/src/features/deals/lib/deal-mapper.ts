import type {
  Deal,
  DealChannelMetadata,
  DealDetailDto,
  DealDto,
  DealEvent,
  DealEventDto,
  DealRole,
  DealStatus,
} from '../types/deal';

const DEFAULT_POST_TYPE = 'NATIVE';

function fallbackChannelTitle(channelId: number): string {
  return `Channel #${channelId}`;
}

function deriveActorRole(
  actorId: number | null | undefined,
  advertiserId: number,
  ownerId: number,
): DealEvent['actorRole'] {
  if (actorId == null) return 'SYSTEM';
  if (actorId === advertiserId) return 'ADVERTISER';
  if (actorId === ownerId) return 'OWNER';
  return 'SYSTEM';
}

function resolveStatus(event: DealEventDto): DealStatus {
  return event.toStatus ?? event.fromStatus ?? 'DRAFT';
}

export function deriveDealRole(
  deal: Pick<DealDto, 'advertiserId' | 'ownerId'>,
  profileId: number | null | undefined,
): DealRole | null {
  if (profileId == null) return null;
  if (deal.advertiserId === profileId) return 'ADVERTISER';
  if (deal.ownerId === profileId) return 'OWNER';
  return null;
}

export function mapDealEventDtoToViewModel(
  event: DealEventDto,
  context?: { advertiserId: number; ownerId: number },
): DealEvent {
  const status = resolveStatus(event);
  const actorRole = context ? deriveActorRole(event.actorId, context.advertiserId, context.ownerId) : 'SYSTEM';

  return {
    id: String(event.id),
    type: event.eventType,
    status,
    actorRole,
    message: null,
    createdAt: event.createdAt,
  };
}

export function mapDealDtoToViewModel(
  deal: DealDto,
  options: {
    profileId: number | null | undefined;
    channel: DealChannelMetadata | null;
  },
): Deal {
  const role = deriveDealRole(deal, options.profileId) ?? 'ADVERTISER';
  const channel = options.channel;

  return {
    id: deal.id,
    status: deal.status,
    channelId: deal.channelId,
    channelTitle: channel?.title ?? fallbackChannelTitle(deal.channelId),
    channelUsername: channel?.username ?? null,
    postType: channel?.postType ?? DEFAULT_POST_TYPE,
    priceNano: deal.amountNano,
    amountNano: deal.amountNano,
    durationHours: channel?.durationHours ?? null,
    postFrequencyHours: channel?.postFrequencyHours ?? null,
    role,
    advertiserId: deal.advertiserId,
    ownerId: deal.ownerId,
    message: null,
    deadlineAt: deal.deadlineAt,
    createdAt: deal.createdAt,
    updatedAt: deal.createdAt,
    version: deal.version,
    commissionRateBp: null,
    commissionNano: null,
    timeline: [],
  };
}

function latestTimelineTimestamp(detail: DealDetailDto): string {
  let latest = detail.createdAt;
  for (const event of detail.timeline) {
    if (event.createdAt > latest) {
      latest = event.createdAt;
    }
  }
  return latest;
}

export function mapDealDetailDtoToViewModel(
  detail: DealDetailDto,
  options: {
    profileId: number | null | undefined;
    channel: DealChannelMetadata | null;
  },
): Deal {
  const base = mapDealDtoToViewModel(detail, options);

  return {
    ...base,
    updatedAt: latestTimelineTimestamp(detail),
    commissionRateBp: detail.commissionRateBp,
    commissionNano: detail.commissionNano,
    timeline: detail.timeline.map((event) =>
      mapDealEventDtoToViewModel(event, {
        advertiserId: detail.advertiserId,
        ownerId: detail.ownerId,
      }),
    ),
  };
}
