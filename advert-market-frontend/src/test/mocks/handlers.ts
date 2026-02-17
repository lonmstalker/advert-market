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

const STORAGE_KEYS = {
  profile: 'msw_profile_state',
  deals: 'msw_deals_state',
  dealTimelines: 'msw_deal_timelines_state',
  registeredChannels: 'msw_registered_channels',
} as const;

const DEAL_STATUSES = [
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

const DEAL_STATUS_SET = new Set<string>(DEAL_STATUSES);
const DEFAULT_COMMISSION_RATE_BP = 1000;

type MockDealState = Omit<(typeof mockDeals)[number], 'status'> & {
  status: string;
  amountNano: number;
  version: number;
};

type LegacyTimelineEvent = (typeof mockDealTimelines)[string]['events'][number];

type DealTimelineEventState = {
  id: number;
  eventType: string;
  fromStatus: string | null;
  toStatus: string | null;
  actorId: number | null;
  createdAt: string;
};

type DealTimelineState = Record<string, DealTimelineEventState[]>;

function toAmountNano(deal: { amountNano?: unknown; priceNano?: unknown }): number {
  if (typeof deal.amountNano === 'number') return deal.amountNano;
  if (typeof deal.priceNano === 'number') return deal.priceNano;
  return 1_000_000_000;
}

function toDealVersion(deal: MockDealState | (typeof mockDeals)[number]): number {
  if ('version' in deal && typeof deal.version === 'number') return deal.version;
  return 1;
}

function normalizeDealRecord(deal: MockDealState | (typeof mockDeals)[number]): MockDealState {
  const amountNano = toAmountNano(deal);
  const version = toDealVersion(deal);
  const status = typeof deal.status === 'string' && DEAL_STATUS_SET.has(deal.status) ? deal.status : 'DRAFT';

  return {
    ...deal,
    status,
    amountNano,
    version,
  };
}

function mapActorRoleToId(role: LegacyTimelineEvent['actorRole'], deal: MockDealState): number | null {
  if (role === 'ADVERTISER') return deal.advertiserId;
  if (role === 'OWNER') return deal.ownerId;
  return null;
}

function parseTimelineEventId(rawId: string, fallback: number): number {
  const digitsOnly = rawId.replace(/\D/g, '');
  const parsed = Number(digitsOnly);
  if (Number.isFinite(parsed) && parsed > 0) return parsed;
  return fallback;
}

function convertLegacyTimelineEvents(events: LegacyTimelineEvent[], deal: MockDealState): DealTimelineEventState[] {
  if (events.length === 0) {
    return [
      {
        id: 1,
        eventType: 'DEAL_STATE_CHANGED',
        fromStatus: null,
        toStatus: deal.status,
        actorId: deal.advertiserId,
        createdAt: deal.createdAt,
      },
    ];
  }

  return events.map((event, index) => {
    const prevStatus = index > 0 ? (events[index - 1]?.status ?? null) : null;
    return {
      id: parseTimelineEventId(event.id, index + 1),
      eventType: event.type,
      fromStatus: prevStatus,
      toStatus: event.status,
      actorId: mapActorRoleToId(event.actorRole, deal),
      createdAt: event.createdAt,
    };
  });
}

function buildDefaultDealTimelines(seedDeals: MockDealState[]): DealTimelineState {
  const timelines: DealTimelineState = {};
  for (const deal of seedDeals) {
    const legacyTimeline = mockDealTimelines[deal.id];
    const events = legacyTimeline?.events ?? [];
    timelines[deal.id] = convertLegacyTimelineEvents(events, deal);
  }
  return timelines;
}

function normalizeStoredTimelineEvents(rawEvents: unknown, deal: MockDealState): DealTimelineEventState[] | null {
  if (!Array.isArray(rawEvents)) return null;

  const normalized: DealTimelineEventState[] = [];
  for (const [index, raw] of rawEvents.entries()) {
    if (typeof raw !== 'object' || raw === null) continue;
    const event = raw as {
      id?: unknown;
      eventType?: unknown;
      fromStatus?: unknown;
      toStatus?: unknown;
      actorId?: unknown;
      createdAt?: unknown;
      type?: unknown;
      status?: unknown;
      actorRole?: unknown;
    };

    // Backward compatibility for old mock timeline shape.
    if (typeof event.type === 'string' && typeof event.status === 'string') {
      const actorRole =
        event.actorRole === 'ADVERTISER' || event.actorRole === 'OWNER' || event.actorRole === 'SYSTEM'
          ? event.actorRole
          : 'SYSTEM';
      normalized.push({
        id:
          typeof event.id === 'string'
            ? parseTimelineEventId(event.id, index + 1)
            : typeof event.id === 'number'
              ? event.id
              : index + 1,
        eventType: event.type,
        fromStatus: index > 0 ? (normalized[index - 1]?.toStatus ?? null) : null,
        toStatus: event.status,
        actorId: mapActorRoleToId(actorRole, deal),
        createdAt: typeof event.createdAt === 'string' ? event.createdAt : deal.createdAt,
      });
      continue;
    }

    if (typeof event.eventType !== 'string') continue;
    if (event.fromStatus != null && typeof event.fromStatus !== 'string') continue;
    if (event.toStatus != null && typeof event.toStatus !== 'string') continue;

    normalized.push({
      id: typeof event.id === 'number' ? event.id : index + 1,
      eventType: event.eventType,
      fromStatus: typeof event.fromStatus === 'string' ? event.fromStatus : null,
      toStatus: typeof event.toStatus === 'string' ? event.toStatus : null,
      actorId: typeof event.actorId === 'number' ? event.actorId : null,
      createdAt: typeof event.createdAt === 'string' ? event.createdAt : deal.createdAt,
    });
  }

  return normalized.length > 0 ? normalized : null;
}

function normalizeDealTimelines(rawState: unknown, seedDeals: MockDealState[]): DealTimelineState {
  const fallback = buildDefaultDealTimelines(seedDeals);
  if (typeof rawState !== 'object' || rawState === null) return fallback;
  const source = rawState as Record<string, unknown>;

  const normalized: DealTimelineState = {};
  for (const deal of seedDeals) {
    const rawEntry = source[deal.id];
    const directEvents = normalizeStoredTimelineEvents(rawEntry, deal);
    if (directEvents) {
      normalized[deal.id] = directEvents;
      continue;
    }

    const nestedEvents =
      typeof rawEntry === 'object' && rawEntry !== null
        ? normalizeStoredTimelineEvents((rawEntry as { events?: unknown }).events, deal)
        : null;
    if (nestedEvents) {
      normalized[deal.id] = nestedEvents;
      continue;
    }

    normalized[deal.id] = fallback[deal.id] ?? [];
  }

  return normalized;
}

const defaultDeals = mockDeals.map((deal) => normalizeDealRecord(deal));

function hasSessionStorage(): boolean {
  return typeof sessionStorage !== 'undefined' && sessionStorage != null;
}

function loadState<T>(key: string, fallback: T): T {
  if (!hasSessionStorage()) return fallback;
  const raw = sessionStorage.getItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function saveState(key: string, value: unknown): void {
  if (!hasSessionStorage()) return;
  sessionStorage.setItem(key, JSON.stringify(value));
}

let profile = loadState<typeof mockProfile>(STORAGE_KEYS.profile, { ...mockProfile });
let deals = loadState<MockDealState[]>(STORAGE_KEYS.deals, defaultDeals).map((deal) => normalizeDealRecord(deal));
let dealTimelines = normalizeDealTimelines(
  loadState<DealTimelineState>(STORAGE_KEYS.dealTimelines, buildDefaultDealTimelines(defaultDeals)),
  deals,
);
let registeredChannels = loadState<typeof mockChannels>(STORAGE_KEYS.registeredChannels, []);

const depositChecks = new Map<string, number>();

function getAllChannels() {
  return [...mockChannels, ...registeredChannels];
}

function syncProfileFromStorage(): void {
  profile = loadState<typeof mockProfile>(STORAGE_KEYS.profile, profile);
}

function currencyForLanguage(languageCode: string): string {
  const normalized = languageCode.toLowerCase();
  if (normalized.startsWith('ru')) return 'RUB';
  if (normalized.startsWith('en')) return 'USD';
  return 'USD';
}

type MockTextEntity = {
  type: string;
  offset: number;
  length: number;
  url?: string;
  language?: string;
};

type MockInlineButton = {
  id?: string;
  text: string;
  url?: string;
};

type MockKeyboardRow = MockInlineButton[];

type MockMediaAsset = {
  id?: string;
  type: 'PHOTO' | 'VIDEO' | 'GIF' | 'DOCUMENT';
  url: string;
  thumbnailUrl?: string;
  fileName?: string;
  fileSize?: string;
  mimeType: string;
  sizeBytes: number;
  caption?: string;
};

type MockCreativeDraft = {
  text: string;
  entities: MockTextEntity[];
  media: MockMediaAsset[];
  keyboardRows: MockKeyboardRow[];
  disableWebPagePreview: boolean;
};

type MockCreativeTemplate = {
  id: string;
  title: string;
  draft: MockCreativeDraft;
  version: number;
  createdAt: string;
  updatedAt: string;
};

type MockCreativeVersion = {
  version: number;
  draft: MockCreativeDraft;
  createdAt: string;
};

const MOCK_VISUAL_DATA_URL =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/a6cAAAAASUVORK5CYII=';

function normalizeMediaType(value: unknown): MockMediaAsset['type'] {
  if (value === 'PHOTO' || value === 'VIDEO' || value === 'GIF' || value === 'DOCUMENT') return value;
  return 'DOCUMENT';
}

function inferMediaTypeFromMime(mimeType: string): MockMediaAsset['type'] {
  if (mimeType.startsWith('image/gif')) return 'GIF';
  if (mimeType.startsWith('image/')) return 'PHOTO';
  if (mimeType.startsWith('video/')) return 'VIDEO';
  return 'DOCUMENT';
}

function defaultMimeByMediaType(type: MockMediaAsset['type']): string {
  if (type === 'PHOTO') return 'image/jpeg';
  if (type === 'GIF') return 'image/gif';
  if (type === 'VIDEO') return 'video/mp4';
  return 'application/octet-stream';
}

function defaultFileNameByMediaType(type: MockMediaAsset['type'], index: number): string {
  if (type === 'PHOTO') return `photo-${index + 1}.jpg`;
  if (type === 'GIF') return `gif-${index + 1}.gif`;
  if (type === 'VIDEO') return `video-${index + 1}.mp4`;
  return `document-${index + 1}.bin`;
}

function formatFileSize(sizeBytes: number): string {
  if (sizeBytes < 1024) return `${sizeBytes} B`;
  const kb = sizeBytes / 1024;
  if (kb < 1024) return `${Math.round(kb)} KB`;
  return `${(kb / 1024).toFixed(1)} MB`;
}

async function readFormDataSafe(request: Request, timeoutMs = 300): Promise<FormData | null> {
  try {
    const result = await Promise.race([
      request.formData(),
      new Promise<null>((resolve) => {
        setTimeout(() => resolve(null), timeoutMs);
      }),
    ]);
    return result instanceof FormData ? result : null;
  } catch {
    return null;
  }
}

async function readBodyTextSafe(request: Request, timeoutMs = 300): Promise<string | null> {
  try {
    const clone = request.clone();
    const result = await Promise.race([
      clone.text(),
      new Promise<null>((resolve) => {
        setTimeout(() => resolve(null), timeoutMs);
      }),
    ]);
    return typeof result === 'string' ? result : null;
  } catch {
    return null;
  }
}

async function fileToDataUrl(file: File): Promise<string | null> {
  try {
    const buffer = await file.arrayBuffer();
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    const mimeType = file.type || 'application/octet-stream';
    return `data:${mimeType};base64,${btoa(binary)}`;
  } catch {
    return null;
  }
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function parseMultipartField(body: string, fieldName: string): string | null {
  const pattern = new RegExp(`name="${escapeRegExp(fieldName)}"\\r\\n\\r\\n([^\\r\\n]+)`, 'm');
  const match = body.match(pattern);
  return match?.[1] ?? null;
}

function parseMultipartFileMeta(body: string): { fileName?: string; mimeType?: string; sizeBytes?: number } {
  const match = body.match(/name="file"; filename="([^"]*)"\r\nContent-Type: ([^\r\n]+)\r\n\r\n([\s\S]*?)\r\n--/m);
  if (!match) return {};
  return {
    fileName: match[1] || undefined,
    mimeType: match[2] || undefined,
    sizeBytes: match[3]?.length ?? undefined,
  };
}

function normalizeKeyboardRows(rawDraft: unknown): MockKeyboardRow[] {
  if (typeof rawDraft !== 'object' || rawDraft === null) return [];
  const draft = rawDraft as { keyboardRows?: unknown; buttons?: unknown };

  const source = Array.isArray(draft.keyboardRows)
    ? draft.keyboardRows
    : Array.isArray(draft.buttons)
      ? Array.isArray(draft.buttons[0])
        ? draft.buttons
        : [draft.buttons]
      : [];

  return source
    .filter(Array.isArray)
    .map((row, rowIndex) =>
      row
        .map<MockInlineButton | null>((button, buttonIndex) => {
          if (typeof button !== 'object' || button === null) return null;
          const rawButton = button as { id?: unknown; text?: unknown; url?: unknown };
          const text = typeof rawButton.text === 'string' ? rawButton.text.trim() : '';
          if (!text) return null;
          const id =
            typeof rawButton.id === 'string' && rawButton.id.trim()
              ? rawButton.id
              : `btn-${rowIndex + 1}-${buttonIndex + 1}`;
          const url = typeof rawButton.url === 'string' && rawButton.url ? rawButton.url : undefined;
          return { id, text, ...(url ? { url } : {}) };
        })
        .filter((button): button is MockInlineButton => button !== null)
        .slice(0, 5),
    )
    .filter((row) => row.length > 0)
    .slice(0, 5);
}

function normalizeMediaAssets(rawDraft: unknown, creativeId: string): MockMediaAsset[] {
  if (typeof rawDraft !== 'object' || rawDraft === null) return [];
  const draft = rawDraft as { media?: unknown };
  if (!Array.isArray(draft.media)) return [];

  return draft.media
    .map<MockMediaAsset | null>((rawItem, index) => {
      if (typeof rawItem !== 'object' || rawItem === null) return null;
      const item = rawItem as {
        id?: unknown;
        type?: unknown;
        url?: unknown;
        thumbnailUrl?: unknown;
        fileName?: unknown;
        fileSize?: unknown;
        mimeType?: unknown;
        sizeBytes?: unknown;
        caption?: unknown;
        fileId?: unknown;
      };

      const type = normalizeMediaType(item.type);
      const id = typeof item.id === 'string' && item.id.trim() ? item.id : `${creativeId}-media-${index + 1}`;
      const fileId = typeof item.fileId === 'string' && item.fileId.trim() ? item.fileId : undefined;
      const url =
        typeof item.url === 'string' && item.url.startsWith('http')
          ? item.url
          : type === 'DOCUMENT'
            ? `https://cdn.mock.example/${fileId ?? id}`
            : MOCK_VISUAL_DATA_URL;
      const mimeType =
        typeof item.mimeType === 'string' && item.mimeType.trim() ? item.mimeType : defaultMimeByMediaType(type);
      const sizeBytes = typeof item.sizeBytes === 'number' && item.sizeBytes >= 0 ? item.sizeBytes : 1024;
      const fileName =
        typeof item.fileName === 'string' && item.fileName.trim()
          ? item.fileName
          : defaultFileNameByMediaType(type, index);
      const fileSize =
        typeof item.fileSize === 'string' && item.fileSize.trim() ? item.fileSize : formatFileSize(sizeBytes);
      const thumbnailUrl =
        typeof item.thumbnailUrl === 'string' && item.thumbnailUrl.startsWith('http')
          ? item.thumbnailUrl
          : type === 'VIDEO'
            ? MOCK_VISUAL_DATA_URL
            : undefined;
      const caption = typeof item.caption === 'string' ? item.caption : undefined;

      return {
        id,
        type,
        url,
        ...(thumbnailUrl ? { thumbnailUrl } : {}),
        fileName,
        fileSize,
        mimeType,
        sizeBytes,
        ...(caption ? { caption } : {}),
      };
    })
    .filter((item): item is MockMediaAsset => item !== null)
    .slice(0, 10);
}

function toCreativeDraft(rawDraft: unknown, creativeId: string): MockCreativeDraft {
  if (typeof rawDraft !== 'object' || rawDraft === null) {
    return {
      text: '',
      entities: [],
      media: [],
      keyboardRows: [],
      disableWebPagePreview: false,
    };
  }

  const draft = rawDraft as {
    text?: unknown;
    entities?: unknown;
    disableWebPagePreview?: unknown;
  };

  const entities = Array.isArray(draft.entities)
    ? draft.entities.filter((entity): entity is MockTextEntity => typeof entity === 'object' && entity !== null)
    : [];

  return {
    text: typeof draft.text === 'string' ? draft.text : '',
    entities,
    media: normalizeMediaAssets(rawDraft, creativeId),
    keyboardRows: normalizeKeyboardRows(rawDraft),
    disableWebPagePreview: Boolean(draft.disableWebPagePreview),
  };
}

function toCreativeTemplate(rawTemplate: unknown): MockCreativeTemplate {
  const template =
    typeof rawTemplate === 'object' && rawTemplate !== null ? (rawTemplate as Record<string, unknown>) : {};
  const id = typeof template.id === 'string' && template.id.trim() ? template.id : `creative-${Date.now()}`;
  return {
    id,
    title: typeof template.title === 'string' ? template.title : 'Untitled',
    draft: toCreativeDraft(template.draft, id),
    version: typeof template.version === 'number' ? template.version : 1,
    createdAt: typeof template.createdAt === 'string' ? template.createdAt : new Date().toISOString(),
    updatedAt: typeof template.updatedAt === 'string' ? template.updatedAt : new Date().toISOString(),
  };
}

function toCreativeVersion(rawVersion: unknown, creativeId: string): MockCreativeVersion {
  const version = typeof rawVersion === 'object' && rawVersion !== null ? (rawVersion as Record<string, unknown>) : {};
  return {
    version: typeof version.version === 'number' ? version.version : 1,
    draft: toCreativeDraft(version.draft, creativeId),
    createdAt: typeof version.createdAt === 'string' ? version.createdAt : new Date().toISOString(),
  };
}

let creativeTemplates: MockCreativeTemplate[] = mockCreativeTemplates.map((template) => toCreativeTemplate(template));
let creativeVersionsById: Record<string, MockCreativeVersion[]> = {
  'creative-3': mockCreativeVersions.map((version) => toCreativeVersion(version, 'creative-3')),
};

export function resetMockState(): void {
  profile = { ...mockProfile };
  deals = mockDeals.map((deal) => normalizeDealRecord(deal));
  dealTimelines = buildDefaultDealTimelines(deals);
  registeredChannels = [];
  depositChecks.clear();
  creativeTemplates = mockCreativeTemplates.map((template) => toCreativeTemplate(template));
  creativeVersionsById = {
    'creative-3': mockCreativeVersions.map((version) => toCreativeVersion(version, 'creative-3')),
  };

  if (!hasSessionStorage()) return;
  sessionStorage.removeItem(STORAGE_KEYS.profile);
  sessionStorage.removeItem(STORAGE_KEYS.deals);
  sessionStorage.removeItem(STORAGE_KEYS.dealTimelines);
  sessionStorage.removeItem(STORAGE_KEYS.registeredChannels);
}

function hasPendingDepositIntent(dealId: string): boolean {
  if (!hasSessionStorage()) return false;
  const raw = sessionStorage.getItem('ton_pending_intent');
  if (!raw) return false;
  try {
    const parsed = JSON.parse(raw) as { dealId?: unknown };
    return typeof parsed.dealId === 'string' && parsed.dealId === dealId;
  } catch {
    return false;
  }
}

function currentProfileId(): number | null {
  syncProfileFromStorage();
  return typeof profile.id === 'number' ? profile.id : null;
}

function isParticipant(deal: Pick<MockDealState, 'advertiserId' | 'ownerId'>, userId: number | null): boolean {
  if (userId == null) return true;
  return deal.advertiserId === userId || deal.ownerId === userId;
}

function toDealDto(deal: MockDealState) {
  return {
    id: deal.id,
    channelId: deal.channelId,
    advertiserId: deal.advertiserId,
    ownerId: deal.ownerId,
    status: deal.status,
    amountNano: deal.amountNano,
    deadlineAt: deal.deadlineAt ?? null,
    createdAt: deal.createdAt,
    version: deal.version,
  };
}

function toDealDetailDto(deal: MockDealState) {
  const commissionNano = Math.floor((deal.amountNano * DEFAULT_COMMISSION_RATE_BP) / 10_000);
  return {
    ...toDealDto(deal),
    commissionRateBp: DEFAULT_COMMISSION_RATE_BP,
    commissionNano,
    timeline: dealTimelines[deal.id] ?? [],
  };
}

function getNextTimelineEventId(dealId: string): number {
  const events = dealTimelines[dealId] ?? [];
  let maxId = 0;
  for (const event of events) {
    if (event.id > maxId) {
      maxId = event.id;
    }
  }
  return maxId + 1;
}

function appendTimelineEvent(dealId: string, event: DealTimelineEventState): void {
  const existing = dealTimelines[dealId] ?? [];
  dealTimelines = {
    ...dealTimelines,
    [dealId]: [...existing, event],
  };
  saveState(STORAGE_KEYS.dealTimelines, dealTimelines);
}

function persistDealsState(): void {
  saveState(STORAGE_KEYS.deals, deals);
}

export const handlers = [
  // POST /auth/login — authenticate via Telegram initData
  http.post(`${API_BASE}/auth/login`, () => {
    if (hasSessionStorage()) {
      sessionStorage.setItem('access_token', mockAuthResponse.accessToken);
    }
    return HttpResponse.json(mockAuthResponse);
  }),

  // GET /profile — current user profile
  http.get(`${API_BASE}/profile`, () => {
    syncProfileFromStorage();
    return HttpResponse.json(profile);
  }),

  // PUT /profile/onboarding — complete onboarding
  http.put(`${API_BASE}/profile/onboarding`, async ({ request }) => {
    syncProfileFromStorage();
    const body = (await request.json()) as { interests: string[] };
    profile = {
      ...profile,
      onboardingCompleted: true,
      interests: body.interests,
    };
    saveState(STORAGE_KEYS.profile, profile);
    return HttpResponse.json(profile);
  }),

  // GET /deals — paginated deal list (single backend list + optional status filter)
  http.get(`${API_BASE}/deals`, ({ request }) => {
    const url = new URL(request.url);
    const limit = Number(url.searchParams.get('limit')) || 20;
    const cursor = url.searchParams.get('cursor');
    const status = url.searchParams.get('status');
    const userId = currentProfileId();

    let filtered = deals.filter((deal) => isParticipant(deal, userId));

    if (status && DEAL_STATUS_SET.has(status)) {
      filtered = filtered.filter((deal) => deal.status === status);
    }

    let startIndex = 0;
    if (cursor) {
      startIndex = filtered.findIndex((deal) => deal.id === cursor) + 1;
    }

    const page = filtered.slice(startIndex, startIndex + limit);
    const hasNext = startIndex + limit < filtered.length;
    const nextCursor = hasNext ? (page.at(-1)?.id ?? null) : null;

    return HttpResponse.json({
      items: page.map((deal) => toDealDto(deal)),
      nextCursor,
      hasNext,
    });
  }),

  // GET /deals/:dealId — deal detail with timeline
  http.get(`${API_BASE}/deals/:dealId`, ({ params }) => {
    const dealId = params.dealId as string;
    const deal = deals.find((candidate) => candidate.id === dealId);
    if (!deal) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }

    const userId = currentProfileId();
    if (!isParticipant(deal, userId)) {
      return HttpResponse.json({ type: 'about:blank', title: 'Forbidden', status: 403 }, { status: 403 });
    }

    return HttpResponse.json(toDealDetailDto(deal));
  }),

  // GET /deals/:dealId/deposit — escrow address + deposit status (TON Connect)
  http.get(`${API_BASE}/deals/:dealId/deposit`, ({ params }) => {
    const dealId = params.dealId as string;
    const deal = deals.find((candidate) => candidate.id === dealId);
    if (!deal) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }

    const userId = currentProfileId();
    if (!isParticipant(deal, userId)) {
      return HttpResponse.json({ type: 'about:blank', title: 'Forbidden', status: 403 }, { status: 403 });
    }

    const amountNano = String(deal.amountNano);
    const escrowAddress = `UQ_MOCK_ESCROW_${dealId}`;
    const expiresAt = new Date(Date.now() + 30 * 60 * 1000).toISOString();

    if (deal.status === 'FUNDED') {
      return HttpResponse.json({
        escrowAddress,
        amountNano,
        dealId,
        status: 'CONFIRMED',
        currentConfirmations: 1,
        requiredConfirmations: 1,
        receivedAmountNano: amountNano,
        txHash: `txhash_${dealId}`,
        expiresAt,
      });
    }

    if (!hasPendingDepositIntent(dealId)) {
      depositChecks.delete(dealId);
      return HttpResponse.json({
        escrowAddress,
        amountNano,
        dealId,
        status: 'AWAITING_PAYMENT',
        currentConfirmations: null,
        requiredConfirmations: null,
        receivedAmountNano: null,
        txHash: null,
        expiresAt,
      });
    }

    const attempt = (depositChecks.get(dealId) ?? 0) + 1;
    depositChecks.set(dealId, attempt);

    if (attempt >= 3) {
      const now = new Date().toISOString();
      if (deal.status !== 'FUNDED') {
        const updated = {
          ...deal,
          status: 'FUNDED',
          updatedAt: now,
          version: deal.version + 1,
        };
        deals = deals.map((item) => (item.id === dealId ? updated : item));
        persistDealsState();
        appendTimelineEvent(dealId, {
          id: getNextTimelineEventId(dealId),
          eventType: 'DEAL_STATE_CHANGED',
          fromStatus: deal.status,
          toStatus: 'FUNDED',
          actorId: null,
          createdAt: now,
        });
      }

      return HttpResponse.json({
        escrowAddress,
        amountNano,
        dealId,
        status: 'CONFIRMED',
        currentConfirmations: 1,
        requiredConfirmations: 1,
        receivedAmountNano: amountNano,
        txHash: `txhash_${dealId}`,
        expiresAt,
      });
    }

    if (attempt >= 2) {
      return HttpResponse.json({
        escrowAddress,
        amountNano,
        dealId,
        status: 'CONFIRMING',
        currentConfirmations: 0,
        requiredConfirmations: 1,
        receivedAmountNano: null,
        txHash: `txhash_${dealId}`,
        expiresAt,
      });
    }

    return HttpResponse.json({
      escrowAddress,
      amountNano,
      dealId,
      status: 'TX_DETECTED',
      currentConfirmations: null,
      requiredConfirmations: null,
      receivedAmountNano: null,
      txHash: `txhash_${dealId}`,
      expiresAt,
    });
  }),

  // POST /deals/:dealId/transition — state transition via targetStatus
  http.post(`${API_BASE}/deals/:dealId/transition`, async ({ params, request }) => {
    const dealId = params.dealId as string;
    const deal = deals.find((candidate) => candidate.id === dealId);
    if (!deal) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }

    const userId = currentProfileId();
    if (!isParticipant(deal, userId)) {
      return HttpResponse.json({ type: 'about:blank', title: 'Forbidden', status: 403 }, { status: 403 });
    }

    const body = (await request.json()) as {
      targetStatus?: unknown;
      reason?: unknown;
      partialRefundNano?: unknown;
      partialPayoutNano?: unknown;
    };
    const targetStatus = typeof body.targetStatus === 'string' ? body.targetStatus : null;

    if (!targetStatus || !DEAL_STATUS_SET.has(targetStatus)) {
      return HttpResponse.json({ type: 'about:blank', title: 'Bad Request', status: 400 }, { status: 400 });
    }

    if (deal.status === targetStatus) {
      return HttpResponse.json({
        status: 'ALREADY_IN_TARGET_STATE',
        newStatus: null,
        currentStatus: deal.status,
      });
    }

    const previousStatus = deal.status;
    const now = new Date().toISOString();
    const updated: MockDealState = {
      ...deal,
      status: targetStatus,
      updatedAt: now,
      version: deal.version + 1,
    };

    deals = deals.map((item) => (item.id === dealId ? updated : item));
    persistDealsState();
    appendTimelineEvent(dealId, {
      id: getNextTimelineEventId(dealId),
      eventType: 'DEAL_STATE_CHANGED',
      fromStatus: previousStatus,
      toStatus: targetStatus,
      actorId: userId,
      createdAt: now,
    });

    return HttpResponse.json({
      status: 'SUCCESS',
      newStatus: targetStatus,
      currentStatus: previousStatus,
    });
  }),

  // --- Channel registration handlers ---

  // POST /channels/verify — verify a channel for registration
  http.post(`${API_BASE}/channels/verify`, async ({ request }) => {
    const body = (await request.json()) as { channelUsername: string };
    const username = body.channelUsername.replace(/^@/, '');

    if (username === 'nonexistent_channel') {
      return HttpResponse.json(
        { type: 'about:blank', title: 'Not Found', status: 404, error_code: 'CHANNEL_NOT_FOUND' },
        { status: 404 },
      );
    }

    const existing = getAllChannels().find((ch) => ch.username === username);
    if (existing) {
      return HttpResponse.json(
        { type: 'about:blank', title: 'Conflict', status: 409, error_code: 'CHANNEL_ALREADY_REGISTERED' },
        { status: 409 },
      );
    }

    const isAdmin = username !== 'no_bot_channel';
    return HttpResponse.json({
      channelId: -(Date.now() % 1_000_000),
      title: `Channel @${username}`,
      username,
      subscriberCount: 500,
      botStatus: {
        isAdmin,
        canPostMessages: isAdmin,
        canDeleteMessages: isAdmin,
        canEditMessages: isAdmin,
        missingPermissions: isAdmin ? [] : ['can_post_messages'],
      },
      userStatus: { isMember: true, role: 'CREATOR' },
    });
  }),

  // POST /channels — register a channel
  http.post(`${API_BASE}/channels`, async ({ request }) => {
    const body = (await request.json()) as {
      channelId: number;
      categories?: string[];
      pricePerPostNano?: number;
    };

    const pricePerPostNano = body.pricePerPostNano ?? 2_000_000_000;

    const alreadyRegistered = getAllChannels().find((ch) => ch.id === body.channelId);
    if (alreadyRegistered) {
      return HttpResponse.json(
        { type: 'about:blank', title: 'Conflict', status: 409, error_code: 'CHANNEL_ALREADY_REGISTERED' },
        { status: 409 },
      );
    }

    const newChannel = {
      id: body.channelId,
      title: `Channel ${body.channelId}`,
      username: `channel_${Math.abs(body.channelId)}`,
      subscriberCount: 500,
      categories: body.categories ?? [],
      pricePerPostNano,
      avgViews: 150,
      engagementRate: 3.0,
      isActive: true,
      isVerified: false,
      language: 'ru',
      ownerId: mockProfile.id,
    };

    registeredChannels = [...registeredChannels, newChannel];
    saveState(STORAGE_KEYS.registeredChannels, registeredChannels);

    return HttpResponse.json(
      {
        id: newChannel.id,
        title: newChannel.title,
        username: newChannel.username,
        subscriberCount: newChannel.subscriberCount,
        categories: newChannel.categories,
        pricePerPostNano: newChannel.pricePerPostNano,
        isActive: true,
        ownerId: mockProfile.id,
        createdAt: new Date().toISOString(),
      },
      { status: 201 },
    );
  }),

  // GET /channels/my — channels owned by current user
  http.get(`${API_BASE}/channels/my`, () => {
    const myChannels = getAllChannels()
      .filter((ch) => 'ownerId' in ch && ch.ownerId === mockProfile.id)
      .map((ch) => ({
        id: ch.id,
        title: ch.title,
        username: ch.username ?? null,
        description: null,
        subscriberCount: ch.subscriberCount,
        categories: ch.categories,
        pricePerPostNano: ch.pricePerPostNano,
        isActive: ch.isActive,
        ownerId: mockProfile.id,
        createdAt: '2026-01-01T00:00:00Z',
      }));
    return HttpResponse.json(myChannels);
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
    const last = page.at(-1);
    const nextCursor = hasNext && last ? String(last.id) : null;

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
    const channel = getAllChannels().find((ch) => ch.id === channelId);

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
    if (profile.currencyMode === 'AUTO') {
      profile = {
        ...profile,
        displayCurrency: currencyForLanguage(body.languageCode),
      };
    }
    saveState(STORAGE_KEYS.profile, profile);
    return HttpResponse.json(profile);
  }),

  // PUT /profile/settings — update locale/currency and notification settings
  http.put(`${API_BASE}/profile/settings`, async ({ request }) => {
    const body = (await request.json()) as {
      displayCurrency?: string;
      currencyMode?: 'AUTO' | 'MANUAL';
      notificationSettings?: typeof profile.notificationSettings;
    };

    if (body.currencyMode === 'AUTO') {
      profile = {
        ...profile,
        currencyMode: 'AUTO',
        displayCurrency: currencyForLanguage(profile.languageCode),
      };
    } else if (body.currencyMode === 'MANUAL') {
      profile = { ...profile, currencyMode: 'MANUAL' };
      if (body.displayCurrency) {
        profile = { ...profile, displayCurrency: body.displayCurrency };
      }
    } else if (body.displayCurrency) {
      // Backward compatibility: legacy clients update displayCurrency only.
      profile = {
        ...profile,
        currencyMode: 'MANUAL',
        displayCurrency: body.displayCurrency,
      };
    }

    if (body.notificationSettings) {
      profile = { ...profile, notificationSettings: body.notificationSettings };
    }
    saveState(STORAGE_KEYS.profile, profile);
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
    const nextCursor = hasNext ? (page.at(-1)?.id ?? null) : null;

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
      startIndex = creativeTemplates.findIndex((c) => c.id === cursor) + 1;
    }

    const page = creativeTemplates.slice(startIndex, startIndex + limit);
    const hasNext = startIndex + limit < creativeTemplates.length;
    const nextCursor = hasNext ? (page.at(-1)?.id ?? null) : null;

    return HttpResponse.json({ items: page, nextCursor, hasNext });
  }),

  // GET /creatives/:id — creative detail
  http.get(`${API_BASE}/creatives/:id`, ({ params }) => {
    const creative = creativeTemplates.find((c) => c.id === params.id);
    if (!creative) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }
    return HttpResponse.json(creative);
  }),

  // POST /creatives — create creative
  http.post(`${API_BASE}/creatives`, async ({ request }) => {
    const body = (await request.json()) as {
      title?: unknown;
      text?: unknown;
      entities?: unknown;
      media?: unknown;
      keyboardRows?: unknown;
      disableWebPagePreview?: unknown;
    };

    const id = `creative-${Date.now()}`;
    const draft = toCreativeDraft(
      {
        text: typeof body.text === 'string' ? body.text : '',
        entities: Array.isArray(body.entities) ? body.entities : [],
        media: Array.isArray(body.media) ? body.media : [],
        keyboardRows: Array.isArray(body.keyboardRows) ? body.keyboardRows : [],
        disableWebPagePreview: Boolean(body.disableWebPagePreview),
      },
      id,
    );

    const created: MockCreativeTemplate = {
      id,
      title: typeof body.title === 'string' && body.title.trim() ? body.title : 'Untitled',
      draft,
      version: 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    creativeTemplates.unshift(created);

    return HttpResponse.json(created, { status: 201 });
  }),

  // PUT /creatives/:id — update creative
  http.put(`${API_BASE}/creatives/:id`, async ({ params, request }) => {
    const creative = creativeTemplates.find((c) => c.id === params.id);
    if (!creative) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }

    const body = (await request.json()) as {
      title?: unknown;
      text?: unknown;
      entities?: unknown;
      media?: unknown;
      keyboardRows?: unknown;
      disableWebPagePreview?: unknown;
    };

    const nextDraft = toCreativeDraft(
      {
        text: typeof body.text === 'string' ? body.text : creative.draft.text,
        entities: Array.isArray(body.entities) ? body.entities : creative.draft.entities,
        media: Array.isArray(body.media) ? body.media : creative.draft.media,
        keyboardRows: Array.isArray(body.keyboardRows) ? body.keyboardRows : creative.draft.keyboardRows,
        disableWebPagePreview:
          typeof body.disableWebPagePreview === 'boolean'
            ? body.disableWebPagePreview
            : creative.draft.disableWebPagePreview,
      },
      creative.id,
    );

    const updated: MockCreativeTemplate = {
      ...creative,
      title: typeof body.title === 'string' && body.title.trim() ? body.title : creative.title,
      draft: nextDraft,
      version: creative.version + 1,
      updatedAt: new Date().toISOString(),
    };

    creativeTemplates = creativeTemplates.map((item) => (item.id === creative.id ? updated : item));
    creativeVersionsById[creative.id] = [
      {
        version: updated.version,
        draft: updated.draft,
        createdAt: updated.updatedAt,
      },
      ...(creativeVersionsById[creative.id] ?? []),
    ];

    return HttpResponse.json(updated);
  }),

  // DELETE /creatives/:id — delete creative
  http.delete(`${API_BASE}/creatives/:id`, ({ params }) => {
    const creative = creativeTemplates.find((c) => c.id === params.id);
    if (!creative) {
      return HttpResponse.json({ type: 'about:blank', title: 'Not Found', status: 404 }, { status: 404 });
    }
    creativeTemplates = creativeTemplates.filter((item) => item.id !== creative.id);
    return new HttpResponse(null, { status: 204 });
  }),

  // GET /creatives/:id/versions — version history
  http.get(`${API_BASE}/creatives/:id/versions`, ({ params }) => {
    const id = params.id as string;
    return HttpResponse.json(creativeVersionsById[id] ?? []);
  }),

  // POST /creatives/media — upload media asset
  http.post(`${API_BASE}/creatives/media`, async ({ request }) => {
    const formData = await readFormDataSafe(request);
    const fileEntry = formData?.get('file');
    const file = fileEntry instanceof File ? fileEntry : null;
    const bodyText = formData ? null : await readBodyTextSafe(request);
    const requestedType = formData?.get('mediaType') ?? (bodyText ? parseMultipartField(bodyText, 'mediaType') : null);
    const fallbackFileMeta = bodyText ? parseMultipartFileMeta(bodyText) : {};
    const type =
      typeof requestedType === 'string' && requestedType
        ? normalizeMediaType(requestedType)
        : file
          ? inferMediaTypeFromMime(file.type || 'application/octet-stream')
          : fallbackFileMeta.mimeType
            ? inferMediaTypeFromMime(fallbackFileMeta.mimeType)
            : 'PHOTO';
    const id = `media-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
    const mimeType = file?.type || fallbackFileMeta.mimeType || defaultMimeByMediaType(type);
    const sizeBytes = file?.size ?? fallbackFileMeta.sizeBytes ?? 1024;
    const captionValue = formData?.get('caption') ?? (bodyText ? parseMultipartField(bodyText, 'caption') : null);
    const caption = typeof captionValue === 'string' && captionValue.trim() ? captionValue : undefined;
    const fileName = file?.name || fallbackFileMeta.fileName || defaultFileNameByMediaType(type, 0);
    const uploadedDataUrl = file && type !== 'DOCUMENT' ? await fileToDataUrl(file) : null;

    const media: MockMediaAsset = {
      id,
      type,
      url:
        type === 'DOCUMENT'
          ? `https://cdn.mock.example/uploads/${id}/${encodeURIComponent(fileName)}`
          : (uploadedDataUrl ?? MOCK_VISUAL_DATA_URL),
      ...(type === 'VIDEO' ? { thumbnailUrl: uploadedDataUrl ?? MOCK_VISUAL_DATA_URL } : {}),
      fileName,
      fileSize: formatFileSize(sizeBytes),
      mimeType,
      sizeBytes,
      ...(caption ? { caption } : {}),
    };

    return HttpResponse.json(media, { status: 201 });
  }),

  // DELETE /creatives/media/:mediaId — delete uploaded media asset
  http.delete(`${API_BASE}/creatives/media/:mediaId`, ({ params }) => {
    if (!params.mediaId) {
      return HttpResponse.json({ type: 'about:blank', title: 'Bad Request', status: 400 }, { status: 400 });
    }
    return new HttpResponse(null, { status: 204 });
  }),

  // POST /deals — create a new deal
  http.post(`${API_BASE}/deals`, async ({ request }) => {
    const body = (await request.json()) as {
      channelId: number;
      pricingRuleId?: number;
      amountNano?: number;
      creativeBrief?: string;
    };

    const channel = mockChannels.find((ch) => ch.id === body.channelId);
    const detail = mockChannelDetails[body.channelId];
    const rule = detail?.pricingRules.find((r) => r.id === body.pricingRuleId);
    const now = new Date().toISOString();
    const amountNano = body.amountNano ?? rule?.priceNano ?? channel?.pricePerPostNano ?? 1_000_000_000;
    const ownerId = detail?.ownerId ?? 1;
    const newDeal: MockDealState = {
      id: `deal-${Date.now()}`,
      status: 'OFFER_PENDING',
      channelId: body.channelId,
      channelTitle: channel?.title ?? `Channel #${body.channelId}`,
      channelUsername: channel?.username ?? null,
      postType: rule?.postTypes?.[0] ?? 'NATIVE',
      priceNano: amountNano,
      role: 'ADVERTISER',
      advertiserId: profile.id,
      ownerId,
      message: body.creativeBrief ?? null,
      deadlineAt: null,
      createdAt: now,
      updatedAt: now,
      amountNano,
      version: 1,
    };

    deals = [newDeal, ...deals];
    dealTimelines = {
      ...dealTimelines,
      [newDeal.id]: [
        {
          id: 1,
          eventType: 'DEAL_STATE_CHANGED',
          fromStatus: null,
          toStatus: 'DRAFT',
          actorId: profile.id,
          createdAt: now,
        },
        {
          id: 2,
          eventType: 'DEAL_STATE_CHANGED',
          fromStatus: 'DRAFT',
          toStatus: 'OFFER_PENDING',
          actorId: profile.id,
          createdAt: now,
        },
      ],
    };
    persistDealsState();
    saveState(STORAGE_KEYS.dealTimelines, dealTimelines);

    return HttpResponse.json(toDealDto(newDeal), { status: 201 });
  }),
];

// --- Helper: filter channels by search params ---

function filterChannels(params: URLSearchParams) {
  let result = [...getAllChannels()];

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
