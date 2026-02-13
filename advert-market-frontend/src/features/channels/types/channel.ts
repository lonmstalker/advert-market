import { z } from 'zod/v4';

// --- Categories (from /api/v1/categories) ---

export const categorySchema = z.object({
  id: z.number(),
  slug: z.string(),
  localizedName: z.record(z.string(), z.string()),
  sortOrder: z.number(),
});

export type Category = z.infer<typeof categorySchema>;

// --- Legacy topic shape (used by detail endpoint) ---

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
  channelId: z.number(),
  name: z.string(),
  description: z.string().nullable().optional(),
  postTypes: z.array(z.string()),
  priceNano: z.number(),
  isActive: z.boolean(),
  sortOrder: z.number(),
});

export type PricingRule = z.infer<typeof pricingRuleSchema>;

// --- Channel (list item — matches backend ChannelListItem) ---

export const channelSchema = z.object({
  id: z.number(),
  title: z.string(),
  username: z.string().optional(),
  inviteLink: z.string().optional(),
  subscriberCount: z.number(),
  categories: z.array(z.string()).default([]),
  pricePerPostNano: z.number().optional(),
  avgViews: z.number().optional(),
  engagementRate: z.number().optional(),
  isActive: z.boolean(),
  isVerified: z.boolean().optional(),
  language: z.string().optional(),
  languages: z.array(z.string()).optional(),
  updatedAt: z.string().optional(),
});

export type Channel = z.infer<typeof channelSchema>;

// --- Channel rules ---

export const channelRulesSchema = z.object({
  prohibitedTopics: z.array(z.string()).optional(),
  maxPostChars: z.number().optional(),
  maxButtons: z.number().optional(),
  mediaAllowed: z.boolean().optional(),
  mediaTypes: z.array(z.string()).optional(),
  maxMediaCount: z.number().optional(),
  linksAllowed: z.boolean().optional(),
  mentionsAllowed: z.boolean().optional(),
  formattingAllowed: z.boolean().optional(),
  customRules: z.string().optional(),
});

export type ChannelRules = z.infer<typeof channelRulesSchema>;

// --- Channel detail ---

export const channelDetailSchema = channelSchema.extend({
  description: z.string().optional(),
  ownerId: z.number(),
  createdAt: z.string(),
  avgReach: z.number().optional(),
  postFrequencyHours: z.number().optional(),
  pricingRules: z.array(pricingRuleSchema),
  topics: z.array(channelTopicSchema).optional().default([]),
  rules: channelRulesSchema.optional(),
  nextAvailableSlot: z.string().optional(),
});

export type ChannelDetail = z.infer<typeof channelDetailSchema>;

// --- Channel team ---

const channelTeamMemberInputSchema = z.object({
  userId: z.number(),
  role: z.enum(['owner', 'manager', 'OWNER', 'MANAGER']),
  rights: z.array(z.string()).default([]),
});

function normalizeRole(role: z.infer<typeof channelTeamMemberInputSchema>['role']): 'owner' | 'manager' {
  return role.toLowerCase() as 'owner' | 'manager';
}

function normalizeRight(right: string): string {
  return right.trim().toLowerCase().replaceAll('-', '_');
}

function normalizeMember(member: z.infer<typeof channelTeamMemberInputSchema>) {
  return {
    userId: member.userId,
    role: normalizeRole(member.role),
    rights: member.rights.map(normalizeRight),
  };
}

export const channelTeamMemberSchema = channelTeamMemberInputSchema.transform(normalizeMember);

const channelTeamLegacySchema = z.object({
  members: z.array(channelTeamMemberInputSchema),
});

const channelTeamDirectSchema = z.array(channelTeamMemberInputSchema);

export const channelTeamSchema = z.union([channelTeamLegacySchema, channelTeamDirectSchema]).transform((payload) => {
  const members = Array.isArray(payload) ? payload : payload.members;
  return {
    members: members.map(normalizeMember),
  };
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
  category?: string;
  categories?: string[];
  languages?: string[];
  minSubs?: number;
  maxSubs?: number;
  minPrice?: number;
  maxPrice?: number;
  sort?: ChannelSort;
};
