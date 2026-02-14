import { HttpResponse, http } from 'msw';
import {
  mockAuthResponse,
  mockCategories,
  mockChannelDetails,
  mockChannels,
  mockChannelTeams,
  mockCreativeTemplates,
  mockCreativeVersions,
  mockDeals,
  mockDealTimelines,
  mockProfile,
  mockTransactionDetail,
  mockTransactions,
  mockWalletSummaryOwner,
} from './data';

const API_BASE = '/api/v1';

let profile = { ...mockProfile };

export const handlers = [
  // POST /auth/login — authenticate via Telegram initData
  http.post(`${API_BASE}/auth/login`, () => {
    sessionStorage.setItem('access_token', mockAuthResponse.accessToken);
    return HttpResponse.json(mockAuthResponse);
  }),

  // GET /profile — current user profile
  http.get(`${API_BASE}/profile`, () => {
    return HttpResponse.json(profile);
  }),

  // PUT /profile/onboarding — complete onboarding
  http.put(`${API_BASE}/profile/onboarding`, async ({ request }) => {
    const body = (await request.json()) as { interests: string[] };
    profile = {
      ...profile,
      onboardingCompleted: true,
      interests: body.interests,
    };
    return HttpResponse.json(profile);
  }),

  // GET /deals — paginated deal list with role filter
  http.get(`${API_BASE}/deals`, ({ request }) => {
    const url = new URL(request.url);
    const role = url.searchParams.get('role');
    const limit = Number(url.searchParams.get('limit')) || 20;
    const cursor = url.searchParams.get('cursor');

    let filtered = [...mockDeals];
    if (role) {
      filtered = filtered.filter((d) => d.role === role);
    }

    let startIndex = 0;
    if (cursor) {
      startIndex = filtered.findIndex((d) => d.id === cursor) + 1;
    }

    const page = filtered.slice(startIndex, startIndex + limit);
    const hasNext = startIndex + limit < filtered.length;
    const nextCursor = hasNext ? page.at(-1)!.id : null;

    return HttpResponse.json({ items: page, nextCursor, hasNext });
  }),

  // GET /deals/:dealId — deal detail
  http.get(`${API_BASE}/deals/:dealId`, ({ params }) => {
    const deal = mockDeals.find((d) => d.id === params.dealId);
    if (!deal) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }
    return HttpResponse.json(deal);
  }),

  // GET /deals/:dealId/timeline — deal timeline events
  http.get(`${API_BASE}/deals/:dealId/timeline`, ({ params }) => {
    const timeline = mockDealTimelines[params.dealId as string];
    if (!timeline) {
      return HttpResponse.json({ events: [] });
    }
    return HttpResponse.json(timeline);
  }),

  // POST /deals/:dealId/transition — deal state transition
  http.post(`${API_BASE}/deals/:dealId/transition`, async ({ params, request }) => {
    const deal = mockDeals.find((d) => d.id === params.dealId);
    if (!deal) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }
    const body = (await request.json()) as { action: string };
    const statusMap: Record<string, string> = {
      accept: 'ACCEPTED',
      reject: 'CANCELLED',
      cancel: 'CANCELLED',
      approve_creative: 'CREATIVE_APPROVED',
      publish: 'PUBLISHED',
      schedule: 'SCHEDULED',
    };
    const newStatus = statusMap[body.action] ?? deal.status;
    return HttpResponse.json({ ...deal, status: newStatus, updatedAt: new Date().toISOString() });
  }),

  // POST /deals/:dealId/negotiate — counter-offer
  http.post(`${API_BASE}/deals/:dealId/negotiate`, async ({ params, request }) => {
    const deal = mockDeals.find((d) => d.id === params.dealId);
    if (!deal) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }
    const body = (await request.json()) as { priceNano: number };
    return HttpResponse.json({
      ...deal,
      status: 'NEGOTIATING',
      priceNano: body.priceNano,
      updatedAt: new Date().toISOString(),
    });
  }),

  // --- Channel handlers ---

  // GET /categories — category list (matches backend ReferenceDataController)
  http.get(`${API_BASE}/categories`, () => {
    return HttpResponse.json(mockCategories);
  }),

  // GET /channels/count — filtered count
  http.get(`${API_BASE}/channels/count`, ({ request }) => {
    const url = new URL(request.url);
    const filtered = filterChannels(url.searchParams);
    return HttpResponse.json(filtered.length);
  }),

  // GET /channels — paginated list with filters
  http.get(`${API_BASE}/channels`, ({ request }) => {
    const url = new URL(request.url);
    const filtered = filterChannels(url.searchParams);

    const limit = Number(url.searchParams.get('limit')) || 20;
    const cursor = url.searchParams.get('cursor');

    let startIndex = 0;
    if (cursor) {
      const cursorId = Number(cursor);
      startIndex = filtered.findIndex((ch) => ch.id === cursorId) + 1;
    }

    const page = filtered.slice(startIndex, startIndex + limit);
    const hasNext = startIndex + limit < filtered.length;
    const nextCursor = hasNext ? String(page.at(-1)!.id) : null;

    return HttpResponse.json({
      items: page,
      nextCursor,
      hasNext,
      total: filtered.length,
    });
  }),

  // GET /channels/:channelId — channel detail
  http.get(`${API_BASE}/channels/:channelId`, ({ params }) => {
    const channelId = Number(params.channelId);
    const channel = mockChannels.find((ch) => ch.id === channelId);

    if (!channel) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }

    const detail = mockChannelDetails[channelId];
    const fallbackCategory = mockCategories.find((c) => channel.categories.includes(c.slug));
    return HttpResponse.json({
      ...channel,
      description: detail?.description ?? '',
      ownerId: detail?.ownerId ?? 1,
      createdAt: detail?.createdAt ?? '2025-01-01T00:00:00Z',
      avgReach: detail?.avgReach ?? Math.round(channel.subscriberCount * 0.3),
      ...(detail?.postFrequencyHours != null ? { postFrequencyHours: detail.postFrequencyHours } : {}),
      pricingRules: detail?.pricingRules ?? [
        {
          id: channelId * 100,
          channelId,
          name: 'Native',
          postTypes: ['NATIVE'],
          priceNano: channel.pricePerPostNano ?? 1_000_000_000,
          isActive: true,
          sortOrder: 1,
        },
      ],
      topics:
        detail?.topics ??
        (fallbackCategory ? [{ slug: fallbackCategory.slug, name: fallbackCategory.localizedName.ru }] : []),
      ...(detail?.rules ? { rules: detail.rules } : {}),
    });
  }),

  // GET /channels/:channelId/team — channel team
  http.get(`${API_BASE}/channels/:channelId/team`, ({ params }) => {
    const channelId = Number(params.channelId);
    const team = mockChannelTeams[channelId];
    return HttpResponse.json(team ?? { members: [] });
  }),

  // PUT /profile/language — update language
  http.put(`${API_BASE}/profile/language`, async ({ request }) => {
    const body = (await request.json()) as { languageCode: string };
    profile = { ...profile, languageCode: body.languageCode };
    return HttpResponse.json(profile);
  }),

  // PUT /profile/settings — update display currency and notification settings
  http.put(`${API_BASE}/profile/settings`, async ({ request }) => {
    const body = (await request.json()) as {
      displayCurrency?: string;
      notificationSettings?: typeof profile.notificationSettings;
    };
    if (body.displayCurrency) {
      profile = { ...profile, displayCurrency: body.displayCurrency };
    }
    if (body.notificationSettings) {
      profile = { ...profile, notificationSettings: body.notificationSettings };
    }
    return HttpResponse.json(profile);
  }),

  // --- Wallet handlers ---

  // GET /wallet/summary — wallet summary
  http.get(`${API_BASE}/wallet/summary`, () => {
    return HttpResponse.json(mockWalletSummaryOwner);
  }),

  // GET /wallet/transactions — paginated transaction list
  http.get(`${API_BASE}/wallet/transactions`, ({ request }) => {
    const url = new URL(request.url);
    const type = url.searchParams.get('type');
    const limit = Number(url.searchParams.get('limit')) || 20;
    const cursor = url.searchParams.get('cursor');

    let filtered = [...mockTransactions];
    if (type) {
      filtered = filtered.filter((tx) => tx.type === type);
    }

    let startIndex = 0;
    if (cursor) {
      startIndex = filtered.findIndex((tx) => tx.id === cursor) + 1;
    }

    const page = filtered.slice(startIndex, startIndex + limit);
    const hasNext = startIndex + limit < filtered.length;
    const nextCursor = hasNext ? page.at(-1)!.id : null;

    return HttpResponse.json({ items: page, nextCursor, hasNext });
  }),

  // GET /wallet/transactions/:txId — transaction detail
  http.get(`${API_BASE}/wallet/transactions/:txId`, ({ params }) => {
    const txId = params.txId as string;
    if (txId === mockTransactionDetail.id) {
      return HttpResponse.json(mockTransactionDetail);
    }
    const tx = mockTransactions.find((t) => t.id === txId);
    if (!tx) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }
    return HttpResponse.json({ ...tx, txHash: null, fromAddress: null, toAddress: null, commissionNano: null });
  }),

  // --- Creative Library handlers ---

  // GET /creatives — paginated creative list
  http.get(`${API_BASE}/creatives`, ({ request }) => {
    const url = new URL(request.url);
    const limit = Number(url.searchParams.get('limit')) || 20;
    const cursor = url.searchParams.get('cursor');

    let startIndex = 0;
    if (cursor) {
      startIndex = mockCreativeTemplates.findIndex((c) => c.id === cursor) + 1;
    }

    const page = mockCreativeTemplates.slice(startIndex, startIndex + limit);
    const hasNext = startIndex + limit < mockCreativeTemplates.length;
    const nextCursor = hasNext ? page.at(-1)!.id : null;

    return HttpResponse.json({ items: page, nextCursor, hasNext });
  }),

  // GET /creatives/:id — creative detail
  http.get(`${API_BASE}/creatives/:id`, ({ params }) => {
    const creative = mockCreativeTemplates.find((c) => c.id === params.id);
    if (!creative) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }
    return HttpResponse.json(creative);
  }),

  // POST /creatives — create creative
  http.post(`${API_BASE}/creatives`, async ({ request }) => {
    const body = (await request.json()) as { title: string; text: string };
    return HttpResponse.json(
      {
        id: `creative-${Date.now()}`,
        title: body.title,
        draft: { text: body.text, entities: [], media: [], buttons: [], disableWebPagePreview: false },
        version: 1,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      { status: 201 },
    );
  }),

  // PUT /creatives/:id — update creative
  http.put(`${API_BASE}/creatives/:id`, async ({ params, request }) => {
    const creative = mockCreativeTemplates.find((c) => c.id === params.id);
    if (!creative) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({
      ...creative,
      ...body,
      version: creative.version + 1,
      updatedAt: new Date().toISOString(),
    });
  }),

  // DELETE /creatives/:id — delete creative
  http.delete(`${API_BASE}/creatives/:id`, ({ params }) => {
    const creative = mockCreativeTemplates.find((c) => c.id === params.id);
    if (!creative) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }
    return new HttpResponse(null, { status: 204 });
  }),

  // GET /creatives/:id/versions — version history
  http.get(`${API_BASE}/creatives/:id/versions`, ({ params }) => {
    if (params.id === 'creative-3') {
      return HttpResponse.json(mockCreativeVersions);
    }
    return HttpResponse.json([]);
  }),

  // POST /deals — create a new deal
  http.post(`${API_BASE}/deals`, async ({ request }) => {
    const body = (await request.json()) as {
      channelId: number;
      pricingRuleId: number;
      message?: string;
    };

    const channel = mockChannels.find((ch) => ch.id === body.channelId);
    const detail = mockChannelDetails[body.channelId];
    const rule = detail?.pricingRules.find((r) => r.id === body.pricingRuleId);

    return HttpResponse.json(
      {
        id: `deal-${Date.now()}`,
        status: 'offer_pending',
        channelId: body.channelId,
        pricingRuleId: body.pricingRuleId,
        priceNano: rule?.priceNano ?? channel?.pricePerPostNano ?? 1_000_000_000,
        createdAt: new Date().toISOString(),
      },
      { status: 201 },
    );
  }),
];

// --- Helper: filter channels by search params ---

function filterChannels(params: URLSearchParams) {
  let result = [...mockChannels];

  const q = params.get('q')?.toLowerCase();
  if (q) {
    result = result.filter((ch) => ch.title.toLowerCase().includes(q) || ch.username?.toLowerCase().includes(q));
  }

  const category = params.get('category');
  if (category) {
    result = result.filter((ch) => ch.categories.includes(category));
  }

  const minSubs = params.get('minSubs');
  if (minSubs) {
    result = result.filter((ch) => ch.subscriberCount >= Number(minSubs));
  }

  const maxSubs = params.get('maxSubs');
  if (maxSubs) {
    result = result.filter((ch) => ch.subscriberCount <= Number(maxSubs));
  }

  const minPrice = params.get('minPrice');
  if (minPrice) {
    result = result.filter((ch) => (ch.pricePerPostNano ?? 0) >= Number(minPrice));
  }

  const maxPrice = params.get('maxPrice');
  if (maxPrice) {
    result = result.filter((ch) => (ch.pricePerPostNano ?? 0) <= Number(maxPrice));
  }

  const sort = params.get('sort');
  if (sort === 'subscribers') {
    result.sort((a, b) => b.subscriberCount - a.subscriberCount);
  } else if (sort === 'price_asc') {
    result.sort((a, b) => (a.pricePerPostNano ?? 0) - (b.pricePerPostNano ?? 0));
  } else if (sort === 'price_desc') {
    result.sort((a, b) => (b.pricePerPostNano ?? 0) - (a.pricePerPostNano ?? 0));
  }

  return result;
}
