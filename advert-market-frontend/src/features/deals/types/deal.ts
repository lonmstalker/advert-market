import { z } from 'zod/v4';

export const DEAL_STATUSES = [
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
  'DISPUTED',
  'CANCELLED',
  'EXPIRED',
  'REFUNDED',
  'PARTIALLY_REFUNDED',
] as const;

export const dealStatusSchema = z.enum(DEAL_STATUSES);
export type DealStatus = z.infer<typeof dealStatusSchema>;

export type DealRole = 'ADVERTISER' | 'OWNER';

export const dealListItemSchema = z.object({
  id: z.string(),
  status: dealStatusSchema,
  channelId: z.number(),
  channelTitle: z.string(),
  channelUsername: z.string().nullable(),
  postType: z.string(),
  priceNano: z.number(),
  durationHours: z.number().nullable().optional(),
  postFrequencyHours: z.number().nullable().optional(),
  role: z.enum(['ADVERTISER', 'OWNER']),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type DealListItem = z.infer<typeof dealListItemSchema>;

export const dealEventSchema = z.object({
  id: z.string(),
  type: z.string(),
  status: dealStatusSchema,
  actorRole: z.enum(['ADVERTISER', 'OWNER', 'SYSTEM']),
  message: z.string().nullable(),
  createdAt: z.string(),
});

export type DealEvent = z.infer<typeof dealEventSchema>;

export const dealSchema = z.object({
  id: z.string(),
  status: dealStatusSchema,
  channelId: z.number(),
  channelTitle: z.string(),
  channelUsername: z.string().nullable(),
  postType: z.string(),
  priceNano: z.number(),
  durationHours: z.number().nullable().optional(),
  postFrequencyHours: z.number().nullable().optional(),
  role: z.enum(['ADVERTISER', 'OWNER']),
  advertiserId: z.number(),
  ownerId: z.number(),
  message: z.string().nullable(),
  deadlineAt: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Deal = z.infer<typeof dealSchema>;

export const dealTimelineSchema = z.object({
  events: z.array(dealEventSchema),
});

export type DealTimeline = z.infer<typeof dealTimelineSchema>;

export type NegotiateRequest = {
  priceNano: number;
  message?: string;
};

export type TransitionRequest = {
  action: string;
  message?: string;
};
