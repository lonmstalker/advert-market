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

// --- Channel topics ---

export const mockTopics = [
  { slug: 'crypto', name: 'Криптовалюта' },
  { slug: 'tech', name: 'Технологии' },
  { slug: 'finance', name: 'Финансы' },
  { slug: 'marketing', name: 'Маркетинг' },
  { slug: 'education', name: 'Образование' },
  { slug: 'entertainment', name: 'Развлечения' },
  { slug: 'news', name: 'Новости' },
  { slug: 'lifestyle', name: 'Лайфстайл' },
  { slug: 'business', name: 'Бизнес' },
  { slug: 'gaming', name: 'Игры' },
];

// --- Mock channels ---

export const mockChannels = [
  {
    id: 1,
    title: 'Crypto News Daily',
    username: 'cryptonewsdaily',
    description: 'Ежедневные новости из мира криптовалют и блокчейна. Обзоры, аналитика, прогнозы.',
    subscriberCount: 125000,
    category: 'crypto',
    pricePerPostNano: 5_000_000_000,
    isActive: true,
    ownerId: 1,
    createdAt: '2025-06-01T10:00:00Z',
  },
  {
    id: 2,
    title: 'Tech Digest',
    username: 'techdigest',
    description: 'Технологические новости, обзоры гаджетов и софта.',
    subscriberCount: 89000,
    category: 'tech',
    pricePerPostNano: 3_000_000_000,
    isActive: true,
    ownerId: 2,
    createdAt: '2025-07-10T12:00:00Z',
  },
  {
    id: 3,
    title: 'AI Weekly',
    username: 'aiweekly',
    description: 'Еженедельные обзоры достижений в области ИИ и машинного обучения.',
    subscriberCount: 67000,
    category: 'tech',
    pricePerPostNano: 4_500_000_000,
    isActive: true,
    ownerId: 3,
    createdAt: '2025-08-05T09:00:00Z',
  },
  {
    id: 4,
    title: 'Finance Pro',
    username: 'financepro',
    description: 'Профессиональная аналитика финансовых рынков.',
    subscriberCount: 210000,
    category: 'finance',
    pricePerPostNano: 8_000_000_000,
    isActive: true,
    ownerId: 4,
    createdAt: '2025-05-20T14:00:00Z',
  },
  {
    id: 5,
    title: 'Marketing Hub',
    username: 'marketinghub',
    description: 'Стратегии digital-маркетинга, кейсы и инструменты.',
    subscriberCount: 45000,
    category: 'marketing',
    pricePerPostNano: 2_000_000_000,
    isActive: true,
    ownerId: 5,
    createdAt: '2025-09-01T08:00:00Z',
  },
  {
    id: 6,
    title: 'Dev Notes',
    username: 'devnotes',
    description: 'Заметки разработчика: паттерны, архитектура, код-ревью.',
    subscriberCount: 32000,
    category: 'tech',
    pricePerPostNano: 1_500_000_000,
    isActive: true,
    ownerId: 6,
    createdAt: '2025-10-12T11:00:00Z',
  },
  {
    id: 7,
    title: 'TON Community',
    username: 'toncommunity',
    description: 'Всё о TON блокчейне: DeFi, NFT, разработка.',
    subscriberCount: 156000,
    category: 'crypto',
    pricePerPostNano: 6_000_000_000,
    isActive: true,
    ownerId: 7,
    createdAt: '2025-04-15T16:00:00Z',
  },
  {
    id: 8,
    title: 'Startup Daily',
    username: 'startupdaily',
    description: 'Новости стартапов, инвестиции, истории успеха.',
    subscriberCount: 78000,
    category: 'business',
    pricePerPostNano: 3_500_000_000,
    isActive: true,
    ownerId: 8,
    createdAt: '2025-06-25T13:00:00Z',
  },
  {
    id: 9,
    title: 'GameDev Channel',
    username: 'gamedevchannel',
    description: 'Разработка игр: Unity, Unreal, инди-проекты.',
    subscriberCount: 23000,
    category: 'gaming',
    pricePerPostNano: 1_000_000_000,
    isActive: true,
    ownerId: 9,
    createdAt: '2025-11-01T10:00:00Z',
  },
  {
    id: 10,
    title: 'EdTech Today',
    username: 'edtechtoday',
    description: 'Образовательные технологии и онлайн-обучение.',
    subscriberCount: 54000,
    category: 'education',
    pricePerPostNano: 2_500_000_000,
    isActive: true,
    ownerId: 10,
    createdAt: '2025-08-20T15:00:00Z',
  },
  {
    id: 11,
    title: 'Lifestyle Digest',
    username: 'lifestyledigest',
    description: 'Путешествия, здоровье, мотивация и саморазвитие.',
    subscriberCount: 340000,
    category: 'lifestyle',
    pricePerPostNano: 10_000_000_000,
    isActive: true,
    ownerId: 11,
    createdAt: '2025-03-10T09:00:00Z',
  },
  {
    id: 12,
    title: 'News Flash',
    username: 'newsflash',
    description: 'Молниеносные новости: политика, экономика, общество.',
    subscriberCount: 490000,
    category: 'news',
    pricePerPostNano: 15_000_000_000,
    isActive: true,
    ownerId: 12,
    createdAt: '2025-02-01T07:00:00Z',
  },
  {
    id: 13,
    title: 'Entertainment Zone',
    username: 'entertainmentzone',
    description: 'Мемы, видео, интересные факты и развлекательный контент.',
    subscriberCount: 520000,
    category: 'entertainment',
    pricePerPostNano: 12_000_000_000,
    isActive: true,
    ownerId: 13,
    createdAt: '2025-01-15T18:00:00Z',
  },
  {
    id: 14,
    title: 'DeFi Insights',
    username: 'defiinsights',
    description: 'Аналитика DeFi протоколов, доходность, риски.',
    subscriberCount: 41000,
    category: 'crypto',
    pricePerPostNano: 3_000_000_000,
    isActive: true,
    ownerId: 14,
    createdAt: '2025-07-30T10:00:00Z',
  },
];

// --- Channel detail data (pricing rules, topics, stats) ---

export const mockChannelDetails: Record<
  number,
  {
    avgReach: number;
    engagementRate: number;
    pricingRules: { id: number; postType: string; priceNano: number }[];
    topics: { slug: string; name: string }[];
  }
> = {
  1: {
    avgReach: 45000,
    engagementRate: 3.6,
    pricingRules: [
      { id: 1, postType: '1/24', priceNano: 5_000_000_000 },
      { id: 2, postType: '2/48', priceNano: 4_000_000_000 },
      { id: 3, postType: 'Нативный', priceNano: 7_000_000_000 },
    ],
    topics: [
      { slug: 'crypto', name: 'Криптовалюта' },
      { slug: 'finance', name: 'Финансы' },
    ],
  },
  2: {
    avgReach: 28000,
    engagementRate: 3.1,
    pricingRules: [
      { id: 4, postType: '1/24', priceNano: 3_000_000_000 },
      { id: 5, postType: '2/48', priceNano: 2_500_000_000 },
    ],
    topics: [{ slug: 'tech', name: 'Технологии' }],
  },
  3: {
    avgReach: 22000,
    engagementRate: 4.8,
    pricingRules: [
      { id: 6, postType: '1/24', priceNano: 4_500_000_000 },
      { id: 7, postType: 'Нативный', priceNano: 6_500_000_000 },
    ],
    topics: [
      { slug: 'tech', name: 'Технологии' },
      { slug: 'education', name: 'Образование' },
    ],
  },
  4: {
    avgReach: 85000,
    engagementRate: 4.1,
    pricingRules: [
      { id: 8, postType: '1/24', priceNano: 8_000_000_000 },
      { id: 9, postType: '2/48', priceNano: 6_000_000_000 },
      { id: 10, postType: 'Нативный', priceNano: 12_000_000_000 },
    ],
    topics: [
      { slug: 'finance', name: 'Финансы' },
      { slug: 'business', name: 'Бизнес' },
    ],
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
