import { z } from 'zod/v4';

// --- Topics (dynamic from backend) ---

export const channelTopicSchema = z.object({
  slug: z.string(),
  name: z.string(),
});

export type ChannelTopic = z.infer<typeof channelTopicSchema>;

// --- Sort values (UI logic, hardcoded) ---

export const channelSortValues = ['relevance', 'subscribers', 'price_asc', 'price_desc', 'er'] as const;

export type ChannelSort = (typeof channelSortValues)[number];

// --- Pricing rule ---

export const pricingRuleSchema = z.object({
  id: z.number(),
  postType: z.string(),
  priceNano: z.number(),
});

export type PricingRule = z.infer<typeof pricingRuleSchema>;

// --- Channel (list item) ---

export const channelSchema = z.object({
  id: z.number(),
  title: z.string(),
  username: z.string().optional(),
  description: z.string().optional(),
  subscriberCount: z.number(),
  category: z.string().optional(),
  pricePerPostNano: z.number().optional(),
  isActive: z.boolean(),
  ownerId: z.number(),
  createdAt: z.string(),
});

export type Channel = z.infer<typeof channelSchema>;

// --- Channel detail ---

export const channelDetailSchema = channelSchema.extend({
  avgReach: z.number().optional(),
  engagementRate: z.number().optional(),
  pricingRules: z.array(pricingRuleSchema),
  topics: z.array(channelTopicSchema),
});

export type ChannelDetail = z.infer<typeof channelDetailSchema>;

// --- Channel team ---

export const channelTeamMemberSchema = z.object({
  userId: z.number(),
  role: z.enum(['owner', 'manager']),
  rights: z.array(z.string()),
});

export const channelTeamSchema = z.object({
  members: z.array(channelTeamMemberSchema),
});

export type ChannelTeamMember = z.infer<typeof channelTeamMemberSchema>;
export type ChannelTeam = z.infer<typeof channelTeamSchema>;

// --- Create deal ---

export const createDealRequestSchema = z.object({
  channelId: z.number(),
  pricingRuleId: z.number(),
  message: z.string().optional(),
});

export type CreateDealRequest = z.infer<typeof createDealRequestSchema>;

export const createDealResponseSchema = z.object({
  id: z.string(),
  status: z.string(),
  channelId: z.number(),
  pricingRuleId: z.number(),
  priceNano: z.number(),
  createdAt: z.string(),
});

export type CreateDealResponse = z.infer<typeof createDealResponseSchema>;

// --- Catalog filters ---

export type CatalogFilters = {
  q?: string;
  topic?: string;
  minSubs?: number;
  maxSubs?: number;
  minPrice?: number;
  maxPrice?: number;
  sort?: ChannelSort;
};
