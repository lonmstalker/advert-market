import type { AuthResponse } from '@/shared/api';

export const mockUser = {
  id: 1,
  telegramId: 123456789,
  username: 'testuser',
  displayName: 'Test User',
  languageCode: 'ru',
  onboardingCompleted: false,
  interests: [] as string[],
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

type MockChannelDetail = {
  description: string;
  ownerId: number;
  createdAt: string;
  avgReach: number;
  pricingRules: { id: number; postType: string; priceNano: number; durationHours?: number; description?: string }[];
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
    pricingRules: [
      { id: 1, postType: 'NATIVE', priceNano: 5_000_000_000, durationHours: 24, description: 'Пост в стиле канала с интеграцией продукта' },
      { id: 2, postType: 'NATIVE', priceNano: 8_000_000_000, durationHours: 48, description: 'Нативный пост с удержанием 48ч' },
      { id: 3, postType: 'STORY', priceNano: 4_000_000_000, durationHours: 24, description: 'Кружок или фото в историях канала' },
      { id: 4, postType: 'REPOST', priceNano: 3_000_000_000, durationHours: 72, description: 'Пересылка вашего поста с комментарием' },
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
      customRules: 'Пост должен быть на тему криптовалют или блокчейна.\nПеред публикацией требуется согласование текста.',
    },
  },
  2: {
    description: 'Технологические новости, обзоры гаджетов и софта.',
    ownerId: 2,
    createdAt: '2025-07-10T12:00:00Z',
    avgReach: 28000,
    pricingRules: [
      { id: 5, postType: 'NATIVE', priceNano: 3_000_000_000, durationHours: 24, description: 'Обзор продукта в формате статьи' },
      { id: 6, postType: 'NATIVE', priceNano: 5_000_000_000, durationHours: 48 },
      { id: 7, postType: 'STORY', priceNano: 2_500_000_000, durationHours: 24 },
    ],
    topics: [{ slug: 'tech', name: 'Технологии' }],
  },
  3: {
    description: 'Еженедельные обзоры достижений в области ИИ и машинного обучения.',
    ownerId: 3,
    createdAt: '2025-08-05T09:00:00Z',
    avgReach: 22000,
    pricingRules: [
      { id: 8, postType: 'NATIVE', priceNano: 4_500_000_000, durationHours: 24, description: 'Развёрнутый обзор AI-инструмента с примерами' },
      { id: 9, postType: 'INTEGRATION', priceNano: 6_500_000_000, description: 'Упоминание продукта внутри тематической статьи' },
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
    pricingRules: [
      { id: 10, postType: 'NATIVE', priceNano: 8_000_000_000, durationHours: 24, description: 'Аналитический пост с упоминанием продукта' },
      { id: 11, postType: 'NATIVE', priceNano: 14_000_000_000, durationHours: 72, description: 'Нативный пост с удержанием 72ч' },
      { id: 12, postType: 'STORY', priceNano: 6_000_000_000, durationHours: 24 },
      { id: 13, postType: 'REVIEW', priceNano: 12_000_000_000, description: 'Подробный обзор с личным мнением автора' },
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
    pricingRules: [
      { id: 14, postType: 'NATIVE', priceNano: 2_000_000_000, durationHours: 24, description: 'Кейс или обзор инструмента' },
      { id: 15, postType: 'MENTION', priceNano: 1_500_000_000, description: 'Упоминание в тематическом посте' },
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