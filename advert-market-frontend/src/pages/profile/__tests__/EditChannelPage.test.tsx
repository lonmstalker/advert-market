import { Route, Routes } from 'react-router';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
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

  it('keeps only one pricing rule expanded at a time', async () => {
    mockFetchChannelDetail.mockResolvedValueOnce({
      id: -1001234567890,
      title: 'Stage Channel',
      username: 'stage_channel',
      subscriberCount: 1234,
      categories: ['crypto'],
      isActive: true,
      isVerified: true,
      ownerId: 1,
      createdAt: '2026-02-17T00:00:00Z',
      pricingRules: [
        {
          id: 11,
          channelId: -1001234567890,
          name: 'Native',
          description: null,
          postTypes: ['NATIVE'],
          priceNano: 2_000_000_000,
          isActive: true,
          sortOrder: 0,
        },
        {
          id: 12,
          channelId: -1001234567890,
          name: 'Story',
          description: null,
          postTypes: ['STORY'],
          priceNano: 4_000_000_000,
          isActive: true,
          sortOrder: 1,
        },
      ],
      topics: [],
      rules: {
        customRules: 'No gambling',
      },
    });

    const { user } = renderWithProviders(
      <Routes>
        <Route path="/profile/channels/:channelId/edit" element={<EditChannelPage />} />
      </Routes>,
      { initialEntries: ['/profile/channels/-1001234567890/edit'] },
    );

    expect(await screen.findByText('Stage Channel')).toBeInTheDocument();

    const firstToggle = await screen.findByTestId('edit-channel-rule-toggle-0');
    const secondToggle = await screen.findByTestId('edit-channel-rule-toggle-1');

    expect(firstToggle).toHaveAttribute('aria-expanded', 'true');
    expect(secondToggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByDisplayValue('2')).toBeInTheDocument();
    expect(screen.queryByDisplayValue('4')).not.toBeInTheDocument();

    await user.click(secondToggle);

    expect(firstToggle).toHaveAttribute('aria-expanded', 'false');
    expect(secondToggle).toHaveAttribute('aria-expanded', 'true');
    await waitFor(() => {
      expect(screen.queryByDisplayValue('2')).not.toBeInTheDocument();
      expect(screen.getByDisplayValue('4')).toBeInTheDocument();
    });
  });
});
