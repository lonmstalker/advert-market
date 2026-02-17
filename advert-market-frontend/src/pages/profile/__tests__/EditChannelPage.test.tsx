import { Route, Routes } from 'react-router';
import { renderWithProviders, screen } from '@/test/test-utils';
import EditChannelPage from '../EditChannelPage';

const mockFetchChannelDetail = vi.fn();

vi.mock('@/features/channels', () => ({
  createChannelPricingRule: vi.fn(),
  deleteChannelPricingRule: vi.fn(),
  fetchCategories: vi.fn(async () => []),
  fetchChannelDetail: (...args: unknown[]) => mockFetchChannelDetail(...args),
  fetchPostTypes: vi.fn(async () => []),
  updateChannel: vi.fn(),
  updateChannelPricingRule: vi.fn(),
}));

vi.mock('@/shared/hooks/use-haptic', () => ({
  useHaptic: () => ({
    impactOccurred: vi.fn(),
    notificationOccurred: vi.fn(),
    selectionChanged: vi.fn(),
  }),
}));

vi.mock('@/shared/hooks/use-toast', () => ({
  useToast: () => ({
    showToast: vi.fn(),
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showInfo: vi.fn(),
  }),
}));

vi.mock('@/shared/ui', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/shared/ui')>();
  return { ...actual, BackButtonHandler: () => null };
});

describe('EditChannelPage', () => {
  beforeEach(() => {
    mockFetchChannelDetail.mockReset();
    mockFetchChannelDetail.mockResolvedValue({
      id: -1001234567890,
      title: 'Stage Channel',
      username: 'stage_channel',
      subscriberCount: 1234,
      categories: ['crypto'],
      isActive: true,
      isVerified: true,
      ownerId: 1,
      createdAt: '2026-02-17T00:00:00Z',
      pricingRules: [],
      topics: [],
      rules: {
        customRules: 'No gambling',
      },
    });
  });

  it('loads detail for negative telegram channel id', async () => {
    renderWithProviders(
      <Routes>
        <Route path="/profile/channels/:channelId/edit" element={<EditChannelPage />} />
      </Routes>,
      { initialEntries: ['/profile/channels/-1001234567890/edit'] },
    );

    expect(await screen.findByText('Stage Channel')).toBeInTheDocument();
    expect(mockFetchChannelDetail).toHaveBeenCalledWith(-1001234567890);
  });
});
