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

const nullableIsoSchema = z
  .string()
  .nullable()
  .optional()
  .transform((value) => value ?? null);

// --- Backend DTOs ---

export const dealDtoSchema = z.object({
  id: z.string().min(1),
  channelId: z.number(),
  advertiserId: z.number(),
  ownerId: z.number(),
  status: dealStatusSchema,
  amountNano: z.number(),
  deadlineAt: nullableIsoSchema,
  createdAt: z.string(),
  version: z.number(),
});

export type DealDto = z.infer<typeof dealDtoSchema>;

export const dealEventDtoSchema = z.object({
  id: z.number(),
  eventType: z.string(),
  fromStatus: dealStatusSchema.nullable().optional(),
  toStatus: dealStatusSchema.nullable().optional(),
  actorId: z.number().nullable().optional(),
  createdAt: z.string(),
});

export type DealEventDto = z.infer<typeof dealEventDtoSchema>;

export const dealDetailDtoSchema = dealDtoSchema.extend({
  commissionRateBp: z.number(),
  commissionNano: z.number(),
  timeline: z.array(dealEventDtoSchema).default([]),
});

export type DealDetailDto = z.infer<typeof dealDetailDtoSchema>;

export const transitionRequestSchema = z.object({
  targetStatus: dealStatusSchema,
  reason: z.string().optional(),
  partialRefundNano: z.number().int().positive().optional(),
  partialPayoutNano: z.number().int().positive().optional(),
});

export type TransitionRequest = z.infer<typeof transitionRequestSchema>;

export const dealTransitionResponseSchema = z.object({
  status: z.string(),
  newStatus: dealStatusSchema
    .nullable()
    .optional()
    .transform((value) => value ?? null),
  currentStatus: dealStatusSchema
    .nullable()
    .optional()
    .transform((value) => value ?? null),
});

export type DealTransitionResponse = z.infer<typeof dealTransitionResponseSchema>;

export const dealDepositInfoSchema = z.object({
  escrowAddress: z.string(),
  amountNano: z.string(),
  dealId: z.string(),
  status: z.enum([
    'AWAITING_PAYMENT',
    'TX_DETECTED',
    'CONFIRMING',
    'AWAITING_OPERATOR_REVIEW',
    'CONFIRMED',
    'EXPIRED',
    'UNDERPAID',
    'OVERPAID',
    'REJECTED',
  ]),
  currentConfirmations: z.number().nullable().optional(),
  requiredConfirmations: z.number().nullable().optional(),
  receivedAmountNano: z.string().nullable().optional(),
  txHash: z.string().nullable().optional(),
  expiresAt: z.string().nullable().optional(),
});

export type DealDepositInfo = z.infer<typeof dealDepositInfoSchema>;

// --- UI ViewModel ---

export const dealEventSchema = z.object({
  id: z.string(),
  type: z.string(),
  status: dealStatusSchema,
  actorRole: z.enum(['ADVERTISER', 'OWNER', 'SYSTEM']).nullable(),
  message: z.string().nullable(),
  createdAt: z.string(),
});

export type DealEvent = z.infer<typeof dealEventSchema>;

export const dealListItemSchema = z.object({
  id: z.string(),
  status: dealStatusSchema,
  channelId: z.number(),
  channelTitle: z.string(),
  channelUsername: z.string().nullable(),
  postType: z.string(),
  priceNano: z.number(),
  amountNano: z.number(),
  durationHours: z.number().nullable().optional(),
  postFrequencyHours: z.number().nullable().optional(),
  role: z.enum(['ADVERTISER', 'OWNER']),
  advertiserId: z.number(),
  ownerId: z.number(),
  message: z.string().nullable(),
  deadlineAt: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
  version: z.number(),
});

export type DealListItem = z.infer<typeof dealListItemSchema>;

export const dealSchema = dealListItemSchema.extend({
  commissionRateBp: z.number().nullable().optional(),
  commissionNano: z.number().nullable().optional(),
  timeline: z.array(dealEventSchema).default([]),
});

export type Deal = z.infer<typeof dealSchema>;

export type DealTimeline = DealEvent[];

export type DealChannelMetadata = {
  title: string;
  username: string | null;
  postType: string | null;
  durationHours: number | null;
  postFrequencyHours: number | null;
};
