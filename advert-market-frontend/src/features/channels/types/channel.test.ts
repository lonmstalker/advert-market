import { describe, expect, it } from 'vitest';
import { channelDetailSchema, channelTeamSchema } from './channel';

function buildDetailPayload(overrides: Record<string, unknown> = {}) {
  return {
    id: 1,
    title: 'Channel',
    subscriberCount: 10_000,
    categories: ['tech'],
    isActive: true,
    ownerId: 1,
    createdAt: '2026-02-13T00:00:00Z',
    pricingRules: [],
    ...overrides,
  };
}

describe('channelDetailSchema', () => {
  it('defaults topics to empty array when backend omits topics', () => {
    const detail = channelDetailSchema.parse(buildDetailPayload());
    expect(detail.topics).toEqual([]);
  });

  it('parses backend rules.customRules for owner note compatibility', () => {
    const detail = channelDetailSchema.parse(
      buildDetailPayload({
        rules: {
          customRules: 'No casino ads',
        },
      }),
    );

    expect(detail.rules?.customRules).toBe('No casino ads');
  });
});

describe('channelTeamSchema', () => {
  it('parses legacy wrapped payload with members key', () => {
    const team = channelTeamSchema.parse({
      members: [{ userId: 1, role: 'owner', rights: ['manage_team'] }],
    });

    expect(team.members).toHaveLength(1);
    expect(team.members[0]).toEqual({
      userId: 1,
      role: 'owner',
      rights: ['manage_team'],
    });
  });

  it('parses backend list payload and normalizes role and rights', () => {
    const team = channelTeamSchema.parse([
      { userId: 2, role: 'OWNER', rights: ['MANAGE_TEAM', 'VIEW_STATS'] },
      { userId: 3, role: 'MANAGER', rights: ['PUBLISH'] },
    ]);

    expect(team.members).toEqual([
      {
        userId: 2,
        role: 'owner',
        rights: ['manage_team', 'view_stats'],
      },
      {
        userId: 3,
        role: 'manager',
        rights: ['publish'],
      },
    ]);
  });
});
