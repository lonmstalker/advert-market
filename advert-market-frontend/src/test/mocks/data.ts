import type { AuthResponse } from '@/shared/api';

export const mockNotificationSettings = {
  deals: { newOffers: true, acceptReject: true, deliveryStatus: true },
  financial: { deposits: true, payouts: true, escrow: true },
  disputes: { opened: true, resolved: true },
};

export const mockUser = {
  id: 1,
  telegramId: 123456789,
  username: 'testuser',
  displayName: 'Test User',
  languageCode: 'ru',
  displayCurrency: 'USD',
  currencyMode: 'AUTO' as 'AUTO' | 'MANUAL',
  notificationSettings: mockNotificationSettings,
  onboardingCompleted: false,
  interests: ['advertiser'] as string[],
  createdAt: '2026-01-15T10:00:00Z',
};

export const mockAuthResponse: AuthResponse = {
  accessToken: 'mock-jwt-token-for-development',
  expiresIn: 86400,
  user: {
    id: mockUser.id,
    username: mockUser.username,
    displayName: mockUser.displayName,
  },
};

export const mockProfile = {
  id: mockUser.id,
  telegramId: mockUser.telegramId,
  username: mockUser.username,
  displayName: mockUser.displayName,
  languageCode: mockUser.languageCode,
  displayCurrency: mockUser.displayCurrency,
  currencyMode: mockUser.currencyMode,
  notificationSettings: mockUser.notificationSettings,
  onboardingCompleted: mockUser.onboardingCompleted,
  interests: mockUser.interests,
  createdAt: mockUser.createdAt,
};

// --- Categories (matches backend CategoryDto) ---

export const mockCategories = [
  { id: 1, slug: 'crypto', localizedName: { ru: 'Криптовалюта', en: 'Crypto' }, sortOrder: 1 },
  { id: 2, slug: 'tech', localizedName: { ru: 'Технологии', en: 'Technology' }, sortOrder: 2 },
  { id: 3, slug: 'finance', localizedName: { ru: 'Финансы', en: 'Finance' }, sortOrder: 3 },
  { id: 4, slug: 'marketing', localizedName: { ru: 'Маркетинг', en: 'Marketing' }, sortOrder: 4 },
  { id: 5, slug: 'education', localizedName: { ru: 'Образование', en: 'Education' }, sortOrder: 5 },
  { id: 6, slug: 'entertainment', localizedName: { ru: 'Развлечения', en: 'Entertainment' }, sortOrder: 6 },
  { id: 7, slug: 'news', localizedName: { ru: 'Новости', en: 'News' }, sortOrder: 7 },
  { id: 8, slug: 'lifestyle', localizedName: { ru: 'Лайфстайл', en: 'Lifestyle' }, sortOrder: 8 },
  { id: 9, slug: 'business', localizedName: { ru: 'Бизнес', en: 'Business' }, sortOrder: 9 },
  { id: 10, slug: 'gaming', localizedName: { ru: 'Игры', en: 'Gaming' }, sortOrder: 10 },
];

// --- Mock channels ---

export const mockChannels = [
  {
    id: 1,
    title: 'Crypto News Daily',
    username: 'cryptonewsdaily',
    subscriberCount: 125000,
    categories: ['crypto', 'finance'],
    pricePerPostNano: 5_000_000_000,
    avgViews: 45000,
    engagementRate: 3.6,
    isActive: true,
    isVerified: true,
    language: 'ru',
  },
  {
    id: 2,
    title: 'Tech Digest',
    username: 'techdigest',
    subscriberCount: 89000,
    categories: ['tech'],
    pricePerPostNano: 3_000_000_000,
    avgViews: 28000,
    engagementRate: 3.1,
    isActive: true,
    isVerified: true,
    language: 'ru',
  },
  {
    id: 3,
    title: 'AI Weekly',
    username: 'aiweekly',
    subscriberCount: 67000,
    categories: ['tech', 'education'],
    pricePerPostNano: 4_500_000_000,
    avgViews: 22000,
    engagementRate: 4.8,
    isActive: true,
    isVerified: false,
    language: 'en',
  },
  {
    id: 4,
    title: 'Finance Pro',
    username: 'financepro',
    subscriberCount: 210000,
    categories: ['finance', 'business'],
    pricePerPostNano: 8_000_000_000,
    avgViews: 85000,
    engagementRate: 4.1,
    isActive: true,
    isVerified: true,
    language: 'ru',
  },
  {
    id: 5,
    title: 'Marketing Hub',
    inviteLink: 'https://t.me/+abc123marketinghub',
    subscriberCount: 45000,
    categories: ['marketing'],
    pricePerPostNano: 2_000_000_000,
    avgViews: 12000,
    engagementRate: 2.7,
    isActive: true,
    isVerified: false,
    language: 'ru',
  },
  {
    id: 6,
    title: 'Dev Notes',
    username: 'devnotes',
    subscriberCount: 32000,
    categories: ['tech'],
    pricePerPostNano: 1_500_000_000,
    avgViews: 9500,
    engagementRate: 5.2,
    isActive: true,
    isVerified: false,
    language: 'en',
  },
  {
    id: 7,
    title: 'TON Community',
    username: 'toncommunity',
    subscriberCount: 156000,
    categories: ['crypto', 'tech'],
    pricePerPostNano: 6_000_000_000,
    avgViews: 52000,
    engagementRate: 3.3,
    isActive: true,
    isVerified: true,
    language: 'ru',
  },
  {
    id: 8,
    title: 'Startup Daily',
    username: 'startupdaily',
    subscriberCount: 78000,
    categories: ['business', 'news'],
    pricePerPostNano: 3_500_000_000,
    avgViews: 25000,
    engagementRate: 3.2,
    isActive: true,
    isVerified: false,
    language: 'ru',
  },
  {
    id: 9,
    title: 'GameDev Channel',
    username: 'gamedevchannel',
    subscriberCount: 23000,
    categories: ['gaming', 'tech'],
    pricePerPostNano: 1_000_000_000,
    avgViews: 8000,
    engagementRate: 6.1,
    isActive: true,
    isVerified: false,
    language: 'en',
  },
  {
    id: 10,
    title: 'EdTech Today',
    username: 'edtechtoday',
    subscriberCount: 54000,
    categories: ['education'],
    pricePerPostNano: 2_500_000_000,
    avgViews: 15000,
    engagementRate: 2.8,
    isActive: true,
    isVerified: false,
    language: 'ru',
  },
  {
    id: 11,
    title: 'Lifestyle Digest',
    username: 'lifestyledigest',
    subscriberCount: 340000,
    categories: ['lifestyle'],
    pricePerPostNano: 10_000_000_000,
    avgViews: 110000,
    engagementRate: 3.2,
    isActive: true,
    isVerified: true,
    language: 'ru',
  },
  {
    id: 12,
    title: 'News Flash',
    username: 'newsflash',
    subscriberCount: 490000,
    categories: ['news'],
    pricePerPostNano: 15_000_000_000,
    avgViews: 180000,
    engagementRate: 3.7,
    isActive: true,
    isVerified: true,
    language: 'ru',
  },
  {
    id: 13,
    title: 'Entertainment Zone',
    username: 'entertainmentzone',
    subscriberCount: 520000,
    categories: ['entertainment'],
    pricePerPostNano: 12_000_000_000,
    avgViews: 200000,
    engagementRate: 3.8,
    isActive: true,
    isVerified: false,
    language: 'en',
  },
  {
    id: 14,
    title: 'DeFi Insights',
    username: 'defiinsights',
    subscriberCount: 41000,
    categories: ['crypto', 'finance'],
    pricePerPostNano: 3_000_000_000,
    avgViews: 14000,
    engagementRate: 3.4,
    isActive: true,
    isVerified: false,
    language: 'ru',
  },
];

// --- Channel detail data (pricing rules, topics, stats, description, owner) ---

type MockPricingRule = {
  id: number;
  channelId: number;
  name: string;
  description?: string | null;
  postTypes: string[];
  priceNano: number;
  isActive: boolean;
  sortOrder: number;
};

type MockChannelDetail = {
  description: string;
  ownerId: number;
  createdAt: string;
  avgReach: number;
  postFrequencyHours?: number;
  pricingRules: MockPricingRule[];
  topics: { slug: string; name: string }[];
  rules?: {
    prohibitedTopics?: string[];
    maxPostChars?: number;
    maxButtons?: number;
    mediaAllowed?: boolean;
    mediaTypes?: string[];
    maxMediaCount?: number;
    linksAllowed?: boolean;
    mentionsAllowed?: boolean;
    formattingAllowed?: boolean;
    customRules?: string;
  };
};

export const mockChannelDetails: Record<number, MockChannelDetail> = {
  1: {
    description: 'Ежедневные новости из мира криптовалют и блокчейна. Обзоры, аналитика, прогнозы.',
    ownerId: 1,
    createdAt: '2025-06-01T10:00:00Z',
    avgReach: 45000,
    postFrequencyHours: 6,
    pricingRules: [
      {
        id: 1,
        channelId: 1,
        name: 'Native 24h',
        postTypes: ['NATIVE'],
        priceNano: 5_000_000_000,
        isActive: true,
        sortOrder: 1,
        description: 'Пост в стиле канала с интеграцией продукта',
      },
      {
        id: 2,
        channelId: 1,
        name: 'Native 48h',
        postTypes: ['NATIVE'],
        priceNano: 8_000_000_000,
        isActive: true,
        sortOrder: 2,
        description: 'Нативный пост с удержанием 48ч',
      },
      {
        id: 3,
        channelId: 1,
        name: 'Story 24h',
        postTypes: ['STORY'],
        priceNano: 4_000_000_000,
        isActive: true,
        sortOrder: 3,
        description: 'Кружок или фото в историях канала',
      },
      {
        id: 4,
        channelId: 1,
        name: 'Repost 72h',
        postTypes: ['REPOST'],
        priceNano: 3_000_000_000,
        isActive: true,
        sortOrder: 4,
        description: 'Пересылка вашего поста с комментарием',
      },
    ],
    topics: [
      { slug: 'crypto', name: 'Криптовалюта' },
      { slug: 'finance', name: 'Финансы' },
    ],
    rules: {
      prohibitedTopics: ['Казино', 'Форекс', 'P2P-обменники'],
      maxPostChars: 2000,
      maxButtons: 3,
      mediaAllowed: true,
      mediaTypes: ['photo', 'video'],
      maxMediaCount: 2,
      linksAllowed: true,
      mentionsAllowed: false,
      formattingAllowed: true,
      customRules:
        'Пост должен быть на тему криптовалют или блокчейна.\nПеред публикацией требуется согласование текста.',
    },
  },
  2: {
    description: 'Технологические новости, обзоры гаджетов и софта.',
    ownerId: 2,
    createdAt: '2025-07-10T12:00:00Z',
    avgReach: 28000,
    postFrequencyHours: 12,
    pricingRules: [
      {
        id: 5,
        channelId: 2,
        name: 'Native 24h',
        postTypes: ['NATIVE'],
        priceNano: 3_000_000_000,
        isActive: true,
        sortOrder: 1,
        description: 'Обзор продукта в формате статьи',
      },
      {
        id: 6,
        channelId: 2,
        name: 'Native 48h',
        postTypes: ['NATIVE'],
        priceNano: 5_000_000_000,
        isActive: true,
        sortOrder: 2,
      },
      {
        id: 7,
        channelId: 2,
        name: 'Story 24h',
        postTypes: ['STORY'],
        priceNano: 2_500_000_000,
        isActive: true,
        sortOrder: 3,
      },
    ],
    topics: [{ slug: 'tech', name: 'Технологии' }],
  },
  3: {
    description: 'Еженедельные обзоры достижений в области ИИ и машинного обучения.',
    ownerId: 3,
    createdAt: '2025-08-05T09:00:00Z',
    avgReach: 22000,
    pricingRules: [
      {
        id: 8,
        channelId: 3,
        name: 'Native 24h',
        postTypes: ['NATIVE'],
        priceNano: 4_500_000_000,
        isActive: true,
        sortOrder: 1,
        description: 'Развёрнутый обзор AI-инструмента с примерами',
      },
      {
        id: 9,
        channelId: 3,
        name: 'Integration',
        postTypes: ['INTEGRATION'],
        priceNano: 6_500_000_000,
        isActive: true,
        sortOrder: 2,
        description: 'Упоминание продукта внутри тематической статьи',
      },
    ],
    topics: [
      { slug: 'tech', name: 'Технологии' },
      { slug: 'education', name: 'Образование' },
    ],
  },
  4: {
    description: 'Профессиональная аналитика финансовых рынков.',
    ownerId: 4,
    createdAt: '2025-05-20T14:00:00Z',
    avgReach: 85000,
    postFrequencyHours: 4,
    pricingRules: [
      {
        id: 10,
        channelId: 4,
        name: 'Native 24h',
        postTypes: ['NATIVE'],
        priceNano: 8_000_000_000,
        isActive: true,
        sortOrder: 1,
        description: 'Аналитический пост с упоминанием продукта',
      },
      {
        id: 11,
        channelId: 4,
        name: 'Native 72h',
        postTypes: ['NATIVE'],
        priceNano: 14_000_000_000,
        isActive: true,
        sortOrder: 2,
        description: 'Нативный пост с удержанием 72ч',
      },
      {
        id: 12,
        channelId: 4,
        name: 'Story 24h',
        postTypes: ['STORY'],
        priceNano: 6_000_000_000,
        isActive: true,
        sortOrder: 3,
      },
      {
        id: 13,
        channelId: 4,
        name: 'Review',
        postTypes: ['REVIEW'],
        priceNano: 12_000_000_000,
        isActive: true,
        sortOrder: 4,
        description: 'Подробный обзор с личным мнением автора',
      },
    ],
    topics: [
      { slug: 'finance', name: 'Финансы' },
      { slug: 'business', name: 'Бизнес' },
    ],
  },
  5: {
    description: 'Закрытое сообщество маркетологов. Кейсы, стратегии, инструменты.',
    ownerId: 5,
    createdAt: '2026-01-20T10:00:00Z',
    avgReach: 12000,
    postFrequencyHours: 48,
    pricingRules: [
      {
        id: 14,
        channelId: 5,
        name: 'Native 24h',
        postTypes: ['NATIVE'],
        priceNano: 2_000_000_000,
        isActive: true,
        sortOrder: 1,
        description: 'Кейс или обзор инструмента',
      },
      {
        id: 15,
        channelId: 5,
        name: 'Mention',
        postTypes: ['MENTION'],
        priceNano: 1_500_000_000,
        isActive: true,
        sortOrder: 2,
        description: 'Упоминание в тематическом посте',
      },
    ],
    topics: [{ slug: 'marketing', name: 'Маркетинг' }],
    rules: {
      maxPostChars: 1500,
      maxButtons: 2,
      mediaAllowed: true,
      mediaTypes: ['photo'],
      maxMediaCount: 1,
      linksAllowed: true,
      mentionsAllowed: true,
      formattingAllowed: false,
      customRules: 'Только маркетинговая тематика. Без кликбейта.',
    },
  },
};

// --- Mock deals ---

export const mockDeals = [
  {
    id: 'deal-1',
    status: 'OFFER_PENDING',
    channelId: 2,
    channelTitle: 'Tech Digest',
    channelUsername: 'techdigest',
    postType: 'NATIVE',
    priceNano: 3_000_000_000,
    durationHours: 24,
    postFrequencyHours: 12,
    role: 'ADVERTISER',
    advertiserId: 1,
    ownerId: 2,
    message: 'Interested in native ad placement for our SaaS product',
    deadlineAt: '2026-02-20T10:00:00Z',
    createdAt: '2026-02-12T10:00:00Z',
    updatedAt: '2026-02-12T10:00:00Z',
  },
  {
    id: 'deal-2',
    status: 'NEGOTIATING',
    channelId: 3,
    channelTitle: 'AI Weekly',
    channelUsername: 'aiweekly',
    postType: 'NATIVE',
    priceNano: 4_500_000_000,
    role: 'ADVERTISER',
    advertiserId: 1,
    ownerId: 3,
    message: 'AI tool review placement',
    deadlineAt: '2026-02-25T10:00:00Z',
    createdAt: '2026-02-10T14:00:00Z',
    updatedAt: '2026-02-11T09:00:00Z',
  },
  {
    id: 'deal-3',
    status: 'AWAITING_PAYMENT',
    channelId: 4,
    channelTitle: 'Finance Pro',
    channelUsername: 'financepro',
    postType: 'NATIVE',
    priceNano: 8_000_000_000,
    role: 'ADVERTISER',
    advertiserId: 1,
    ownerId: 4,
    message: null,
    deadlineAt: '2026-02-28T10:00:00Z',
    createdAt: '2026-02-08T11:00:00Z',
    updatedAt: '2026-02-12T15:00:00Z',
  },
  {
    id: 'deal-4',
    status: 'FUNDED',
    channelId: 1,
    channelTitle: 'Crypto News Daily',
    channelUsername: 'cryptonewsdaily',
    postType: 'NATIVE',
    priceNano: 5_000_000_000,
    durationHours: 24,
    postFrequencyHours: 6,
    role: 'OWNER',
    advertiserId: 10,
    ownerId: 1,
    message: 'Crypto exchange review',
    deadlineAt: '2026-03-01T10:00:00Z',
    createdAt: '2026-02-05T08:00:00Z',
    updatedAt: '2026-02-11T16:00:00Z',
  },
  {
    id: 'deal-5',
    status: 'SCHEDULED',
    channelId: 1,
    channelTitle: 'Crypto News Daily',
    channelUsername: 'cryptonewsdaily',
    postType: 'STORY',
    priceNano: 4_000_000_000,
    role: 'OWNER',
    advertiserId: 11,
    ownerId: 1,
    message: 'DeFi platform promo story',
    deadlineAt: '2026-02-22T10:00:00Z',
    createdAt: '2026-02-03T12:00:00Z',
    updatedAt: '2026-02-12T10:30:00Z',
  },
  {
    id: 'deal-6',
    status: 'PUBLISHED',
    channelId: 7,
    channelTitle: 'TON Community',
    channelUsername: 'toncommunity',
    postType: 'NATIVE',
    priceNano: 6_000_000_000,
    role: 'OWNER',
    advertiserId: 12,
    ownerId: 1,
    message: null,
    deadlineAt: null,
    createdAt: '2026-01-20T09:00:00Z',
    updatedAt: '2026-02-10T14:00:00Z',
  },
  {
    id: 'deal-7',
    status: 'COMPLETED_RELEASED',
    channelId: 2,
    channelTitle: 'Tech Digest',
    channelUsername: 'techdigest',
    postType: 'NATIVE',
    priceNano: 3_000_000_000,
    role: 'ADVERTISER',
    advertiserId: 1,
    ownerId: 2,
    message: 'Previous successful campaign',
    deadlineAt: null,
    createdAt: '2026-01-10T10:00:00Z',
    updatedAt: '2026-02-01T12:00:00Z',
  },
  {
    id: 'deal-8',
    status: 'CANCELLED',
    channelId: 5,
    channelTitle: 'Marketing Hub',
    channelUsername: null,
    postType: 'NATIVE',
    priceNano: 2_000_000_000,
    role: 'ADVERTISER',
    advertiserId: 1,
    ownerId: 5,
    message: null,
    deadlineAt: null,
    createdAt: '2026-01-05T10:00:00Z',
    updatedAt: '2026-01-06T11:00:00Z',
  },
];

type MockDealEvent = {
  id: string;
  type: string;
  status: string;
  actorRole: 'ADVERTISER' | 'OWNER' | 'SYSTEM';
  message: string | null;
  createdAt: string;
};

export const mockDealTimelines: Record<string, { events: MockDealEvent[] }> = {
  'deal-1': {
    events: [
      {
        id: 'evt-1-1',
        type: 'CREATED',
        status: 'DRAFT' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-02-12T10:00:00Z',
      },
      {
        id: 'evt-1-2',
        type: 'OFFER_SENT',
        status: 'OFFER_PENDING' as const,
        actorRole: 'ADVERTISER' as const,
        message: 'Interested in native ad placement',
        createdAt: '2026-02-12T10:00:30Z',
      },
    ],
  },
  'deal-2': {
    events: [
      {
        id: 'evt-2-1',
        type: 'CREATED',
        status: 'DRAFT' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-02-10T14:00:00Z',
      },
      {
        id: 'evt-2-2',
        type: 'OFFER_SENT',
        status: 'OFFER_PENDING' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-02-10T14:00:30Z',
      },
      {
        id: 'evt-2-3',
        type: 'COUNTER_OFFER',
        status: 'NEGOTIATING' as const,
        actorRole: 'OWNER' as const,
        message: 'Can we do 5 TON instead?',
        createdAt: '2026-02-11T09:00:00Z',
      },
    ],
  },
  'deal-3': {
    events: [
      {
        id: 'evt-3-1',
        type: 'CREATED',
        status: 'DRAFT' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-02-08T11:00:00Z',
      },
      {
        id: 'evt-3-2',
        type: 'OFFER_SENT',
        status: 'OFFER_PENDING' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-02-08T11:00:30Z',
      },
      {
        id: 'evt-3-3',
        type: 'ACCEPTED',
        status: 'ACCEPTED' as const,
        actorRole: 'OWNER' as const,
        message: null,
        createdAt: '2026-02-09T10:00:00Z',
      },
      {
        id: 'evt-3-4',
        type: 'PAYMENT_REQUESTED',
        status: 'AWAITING_PAYMENT' as const,
        actorRole: 'SYSTEM' as const,
        message: null,
        createdAt: '2026-02-09T10:00:30Z',
      },
    ],
  },
  'deal-4': {
    events: [
      {
        id: 'evt-4-1',
        type: 'CREATED',
        status: 'DRAFT' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-02-05T08:00:00Z',
      },
      {
        id: 'evt-4-2',
        type: 'OFFER_SENT',
        status: 'OFFER_PENDING' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-02-05T08:00:30Z',
      },
      {
        id: 'evt-4-3',
        type: 'ACCEPTED',
        status: 'ACCEPTED' as const,
        actorRole: 'OWNER' as const,
        message: null,
        createdAt: '2026-02-06T09:00:00Z',
      },
      {
        id: 'evt-4-4',
        type: 'PAYMENT_REQUESTED',
        status: 'AWAITING_PAYMENT' as const,
        actorRole: 'SYSTEM' as const,
        message: null,
        createdAt: '2026-02-06T09:00:30Z',
      },
      {
        id: 'evt-4-5',
        type: 'FUNDED',
        status: 'FUNDED' as const,
        actorRole: 'SYSTEM' as const,
        message: 'Escrow funded via TON',
        createdAt: '2026-02-07T11:00:00Z',
      },
    ],
  },
  'deal-5': {
    events: [
      {
        id: 'evt-5-1',
        type: 'CREATED',
        status: 'DRAFT' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-02-03T12:00:00Z',
      },
      {
        id: 'evt-5-2',
        type: 'OFFER_SENT',
        status: 'OFFER_PENDING' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-02-03T12:00:30Z',
      },
      {
        id: 'evt-5-3',
        type: 'ACCEPTED',
        status: 'ACCEPTED' as const,
        actorRole: 'OWNER' as const,
        message: null,
        createdAt: '2026-02-04T08:00:00Z',
      },
      {
        id: 'evt-5-4',
        type: 'PAYMENT_REQUESTED',
        status: 'AWAITING_PAYMENT' as const,
        actorRole: 'SYSTEM' as const,
        message: null,
        createdAt: '2026-02-04T08:00:30Z',
      },
      {
        id: 'evt-5-5',
        type: 'FUNDED',
        status: 'FUNDED' as const,
        actorRole: 'SYSTEM' as const,
        message: null,
        createdAt: '2026-02-05T10:00:00Z',
      },
      {
        id: 'evt-5-6',
        type: 'SCHEDULED',
        status: 'SCHEDULED' as const,
        actorRole: 'ADVERTISER' as const,
        message: 'Banner uploaded',
        createdAt: '2026-02-12T10:30:00Z',
      },
    ],
  },
  'deal-6': {
    events: [
      {
        id: 'evt-6-1',
        type: 'CREATED',
        status: 'DRAFT' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-01-20T09:00:00Z',
      },
      {
        id: 'evt-6-2',
        type: 'OFFER_SENT',
        status: 'OFFER_PENDING' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-01-20T09:00:30Z',
      },
      {
        id: 'evt-6-3',
        type: 'ACCEPTED',
        status: 'ACCEPTED' as const,
        actorRole: 'OWNER' as const,
        message: null,
        createdAt: '2026-01-21T10:00:00Z',
      },
      {
        id: 'evt-6-4',
        type: 'PAYMENT_REQUESTED',
        status: 'AWAITING_PAYMENT' as const,
        actorRole: 'SYSTEM' as const,
        message: null,
        createdAt: '2026-01-21T10:00:30Z',
      },
      {
        id: 'evt-6-5',
        type: 'FUNDED',
        status: 'FUNDED' as const,
        actorRole: 'SYSTEM' as const,
        message: null,
        createdAt: '2026-01-22T14:00:00Z',
      },
      {
        id: 'evt-6-6',
        type: 'SCHEDULED',
        status: 'SCHEDULED' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-01-25T11:00:00Z',
      },
      {
        id: 'evt-6-7',
        type: 'PUBLISHED',
        status: 'PUBLISHED' as const,
        actorRole: 'OWNER' as const,
        message: null,
        createdAt: '2026-01-26T09:00:00Z',
      },
      {
        id: 'evt-6-8',
        type: 'PUBLISHED',
        status: 'PUBLISHED' as const,
        actorRole: 'OWNER' as const,
        message: null,
        createdAt: '2026-02-10T14:00:00Z',
      },
    ],
  },
  'deal-7': {
    events: [
      {
        id: 'evt-7-1',
        type: 'CREATED',
        status: 'DRAFT' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-01-10T10:00:00Z',
      },
      {
        id: 'evt-7-2',
        type: 'OFFER_SENT',
        status: 'OFFER_PENDING' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-01-10T10:00:30Z',
      },
      {
        id: 'evt-7-3',
        type: 'ACCEPTED',
        status: 'ACCEPTED' as const,
        actorRole: 'OWNER' as const,
        message: null,
        createdAt: '2026-01-11T08:00:00Z',
      },
      {
        id: 'evt-7-4',
        type: 'PAYMENT_REQUESTED',
        status: 'AWAITING_PAYMENT' as const,
        actorRole: 'SYSTEM' as const,
        message: null,
        createdAt: '2026-01-11T08:00:30Z',
      },
      {
        id: 'evt-7-5',
        type: 'FUNDED',
        status: 'FUNDED' as const,
        actorRole: 'SYSTEM' as const,
        message: null,
        createdAt: '2026-01-12T12:00:00Z',
      },
      {
        id: 'evt-7-6',
        type: 'SCHEDULED',
        status: 'SCHEDULED' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-01-15T09:00:00Z',
      },
      {
        id: 'evt-7-7',
        type: 'PUBLISHED',
        status: 'PUBLISHED' as const,
        actorRole: 'OWNER' as const,
        message: null,
        createdAt: '2026-01-16T11:00:00Z',
      },
      {
        id: 'evt-7-8',
        type: 'PUBLISHED',
        status: 'PUBLISHED' as const,
        actorRole: 'OWNER' as const,
        message: null,
        createdAt: '2026-01-20T14:00:00Z',
      },
      {
        id: 'evt-7-9',
        type: 'DELIVERY_VERIFIED',
        status: 'DELIVERY_VERIFYING' as const,
        actorRole: 'SYSTEM' as const,
        message: null,
        createdAt: '2026-01-20T14:30:00Z',
      },
      {
        id: 'evt-7-10',
        type: 'COMPLETED',
        status: 'COMPLETED_RELEASED' as const,
        actorRole: 'SYSTEM' as const,
        message: 'Funds released to owner',
        createdAt: '2026-02-01T12:00:00Z',
      },
    ],
  },
  'deal-8': {
    events: [
      {
        id: 'evt-8-1',
        type: 'CREATED',
        status: 'DRAFT' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-01-05T10:00:00Z',
      },
      {
        id: 'evt-8-2',
        type: 'OFFER_SENT',
        status: 'OFFER_PENDING' as const,
        actorRole: 'ADVERTISER' as const,
        message: null,
        createdAt: '2026-01-05T10:00:30Z',
      },
      {
        id: 'evt-8-3',
        type: 'CANCELLED',
        status: 'CANCELLED' as const,
        actorRole: 'ADVERTISER' as const,
        message: 'Changed plans',
        createdAt: '2026-01-06T11:00:00Z',
      },
    ],
  },
};

// --- Wallet summaries ---

export const mockWalletSummaryOwner = {
  earnedTotalNano: '15000000000',
  inEscrowNano: '5000000000',
  spentTotalNano: '0',
  activeEscrowNano: '0',
  activeDealsCount: 2,
  completedDealsCount: 5,
};

export const mockWalletSummaryAdvertiser = {
  earnedTotalNano: '0',
  inEscrowNano: '0',
  spentTotalNano: '25000000000',
  activeEscrowNano: '8000000000',
  activeDealsCount: 3,
  completedDealsCount: 4,
};

export const mockWalletSummaryEmpty = {
  earnedTotalNano: '0',
  inEscrowNano: '0',
  spentTotalNano: '0',
  activeEscrowNano: '0',
  activeDealsCount: 0,
  completedDealsCount: 0,
};

// --- Transactions ---

const today = new Date().toISOString().slice(0, 10);
const yesterday = new Date(Date.now() - 86_400_000).toISOString().slice(0, 10);

export const mockTransactions = [
  {
    id: 'tx-1',
    type: 'escrow_deposit',
    status: 'confirmed',
    amountNano: '5000000000',
    direction: 'expense',
    dealId: 'deal-1',
    channelTitle: 'Crypto News Daily',
    description: 'Escrow deposit for deal',
    createdAt: `${today}T10:00:00Z`,
  },
  {
    id: 'tx-2',
    type: 'payout',
    status: 'confirmed',
    amountNano: '4750000000',
    direction: 'income',
    dealId: 'deal-7',
    channelTitle: 'Tech Digest',
    description: 'Payout for completed deal',
    createdAt: `${today}T08:30:00Z`,
  },
  {
    id: 'tx-3',
    type: 'refund',
    status: 'pending',
    amountNano: '3000000000',
    direction: 'income',
    dealId: 'deal-8',
    channelTitle: null,
    description: 'Refund for cancelled deal',
    createdAt: `${yesterday}T14:00:00Z`,
  },
  {
    id: 'tx-4',
    type: 'commission',
    status: 'confirmed',
    amountNano: '250000000',
    direction: 'expense',
    dealId: 'deal-7',
    channelTitle: 'Tech Digest',
    description: 'Platform commission',
    createdAt: `${yesterday}T08:30:00Z`,
  },
  {
    id: 'tx-5',
    type: 'escrow_deposit',
    status: 'failed',
    amountNano: '8000000000',
    direction: 'expense',
    dealId: 'deal-3',
    channelTitle: 'Finance Pro',
    description: 'Failed escrow deposit',
    createdAt: '2026-01-15T12:00:00Z',
  },
  {
    id: 'tx-6',
    type: 'payout',
    status: 'pending',
    amountNano: '6000000000',
    direction: 'income',
    dealId: 'deal-6',
    channelTitle: 'TON Community',
    description: 'Pending payout',
    createdAt: '2026-01-10T09:00:00Z',
  },
];

const tx0 = mockTransactions.at(0);
if (!tx0) throw new Error('Expected mockTransactions[0] to exist');
const tx2 = mockTransactions.at(2);
if (!tx2) throw new Error('Expected mockTransactions[2] to exist');

export const mockTransactionDetail = {
  ...tx0,
  txHash: 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2',
  fromAddress: 'EQBvW8Z5huBkMJYdnfAEFYpzHC2p0y3wR6Qf5eJYhS1eN0Yz',
  toAddress: 'EQAo92DYMokBh2HJiIXLfhE0qiG0mF3KxhNZ2W1ghT3xQ4Rn',
  commissionNano: '250000000',
};

export const mockTransactionDetailMinimal = {
  ...tx2,
  txHash: null,
  fromAddress: null,
  toAddress: null,
  commissionNano: null,
};

// --- Creative Templates ---

export const mockCreativeTemplates = [
  {
    id: 'creative-1',
    title: 'Crypto Exchange Promo',
    draft: {
      text: 'Trade crypto with zero fees! Join the fastest exchange in TON ecosystem. Start now and get 100 TON bonus.',
      entities: [
        { type: 'BOLD', offset: 0, length: 30 },
        { type: 'ITALIC', offset: 31, length: 50 },
        { type: 'TEXT_LINK', offset: 82, length: 9, url: 'https://example.com/signup' },
      ],
      media: [{ type: 'PHOTO', fileId: 'AgACAgIAAx0CZ', caption: 'Exchange banner' }],
      buttons: [
        { text: 'Start Trading', url: 'https://example.com/trade' },
        { text: 'Learn More', url: 'https://example.com/about' },
      ],
      disableWebPagePreview: true,
    },
    version: 2,
    createdAt: '2026-02-10T10:00:00Z',
    updatedAt: '2026-02-12T14:00:00Z',
  },
  {
    id: 'creative-2',
    title: 'AI Tool Review',
    draft: {
      text: 'New AI assistant that actually works. We tested it for 30 days — here are the results.',
      entities: [
        { type: 'BOLD', offset: 0, length: 38 },
        { type: 'UNDERLINE', offset: 43, length: 42 },
      ],
      media: [],
      buttons: [{ text: 'Try Free', url: 'https://example.com/ai-tool' }],
      disableWebPagePreview: false,
    },
    version: 1,
    createdAt: '2026-02-08T15:00:00Z',
    updatedAt: '2026-02-08T15:00:00Z',
  },
  {
    id: 'creative-3',
    title: 'DeFi Platform Launch',
    draft: {
      text: 'Introducing a new DeFi platform on TON blockchain.\n\nFeatures:\n- Staking with 15% APY\n- Instant swaps\n- No KYC required\n\nJoin the waitlist today!',
      entities: [
        { type: 'BOLD', offset: 0, length: 50 },
        { type: 'CODE', offset: 63, length: 18 },
        { type: 'SPOILER', offset: 119, length: 27 },
      ],
      media: [
        { type: 'PHOTO', fileId: 'AgACAgIAAx1CY', caption: 'Platform screenshot' },
        { type: 'VIDEO', fileId: 'BAACAgIAAx0CX', caption: 'Demo video' },
      ],
      buttons: [{ text: 'Join Waitlist', url: 'https://example.com/waitlist' }],
      disableWebPagePreview: true,
    },
    version: 3,
    createdAt: '2026-02-01T09:00:00Z',
    updatedAt: '2026-02-13T11:00:00Z',
  },
];

const defiTemplate = mockCreativeTemplates.at(2);
if (!defiTemplate) throw new Error('Expected mockCreativeTemplates[2] to exist');

export const mockCreativeVersions = [
  {
    version: 3,
    draft: defiTemplate.draft,
    createdAt: '2026-02-13T11:00:00Z',
  },
  {
    version: 2,
    draft: { ...defiTemplate.draft, text: 'Introducing a new DeFi platform.' },
    createdAt: '2026-02-10T14:00:00Z',
  },
  {
    version: 1,
    draft: { ...defiTemplate.draft, text: 'DeFi platform coming soon.' },
    createdAt: '2026-02-01T09:00:00Z',
  },
];

// --- Channel teams ---

export const mockChannelTeams: Record<number, { members: { userId: number; role: string; rights: string[] }[] }> = {
  1: {
    members: [{ userId: 1, role: 'owner', rights: ['manage_listings', 'manage_team', 'view_analytics'] }],
  },
  2: {
    members: [{ userId: 2, role: 'owner', rights: ['manage_listings', 'manage_team', 'view_analytics'] }],
  },
  3: {
    members: [
      { userId: 3, role: 'owner', rights: ['manage_listings', 'manage_team', 'view_analytics'] },
      { userId: 1, role: 'manager', rights: ['view_analytics'] },
    ],
  },
};
