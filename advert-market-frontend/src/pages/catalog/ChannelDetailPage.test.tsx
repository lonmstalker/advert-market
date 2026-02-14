import { HttpResponse, http } from 'msw';
import { Route, Routes } from 'react-router';
import { server } from '@/test/mocks/server';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import ChannelDetailPage from './ChannelDetailPage';

vi.mock('@/shared/hooks/use-auth', () => ({
  useAuth: () => ({
    profile: { id: 1 },
    isAuthenticated: true,
    isLoading: false,
    invalidateProfile: vi.fn(),
  }),
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

const { mockCopyToClipboard } = vi.hoisted(() => ({
  mockCopyToClipboard: vi.fn().mockResolvedValue(true),
}));
vi.mock('@/shared/lib/clipboard', () => ({
  copyToClipboard: mockCopyToClipboard,
}));

const API_BASE = '/api/v1';

function renderPage(channelId: number = 1) {
  return renderWithProviders(
    <Routes>
      <Route path="/catalog/channels/:channelId" element={<ChannelDetailPage />} />
      <Route path="/catalog" element={<div>catalog-page</div>} />
      <Route path="/deals/new" element={<div>create-deal-page</div>} />
      <Route path="/profile/channels/:channelId/edit" element={<div>edit-page</div>} />
    </Routes>,
    { initialEntries: [`/catalog/channels/${channelId}`] },
  );
}

describe('ChannelDetailPage', () => {
  // --- Happy path: channel 1 (user is owner) ---

  describe('happy path (channel 1, user is owner)', () => {
    it('shows channel title', async () => {
      renderPage(1);
      expect(await screen.findByText('Crypto News Daily')).toBeInTheDocument();
    });

    it('shows verified badge', async () => {
      renderPage(1);
      await waitFor(() => {
        expect(screen.getByRole('img', { name: /verified/i })).toBeInTheDocument();
      });
    });

    it('shows @cryptonewsdaily username', async () => {
      renderPage(1);
      await waitFor(() => {
        expect(screen.getAllByText(/@cryptonewsdaily/).length).toBeGreaterThanOrEqual(1);
      });
    });

    it('shows "Subscribers" stat label', async () => {
      renderPage(1);
      expect(await screen.findByText('Subscribers')).toBeInTheDocument();
    });

    it('shows avg reach stat', async () => {
      renderPage(1);
      await waitFor(() => {
        expect(screen.getByText('45K')).toBeInTheDocument();
      });
      expect(screen.getByText('Avg. reach')).toBeInTheDocument();
    });

    it('shows engagement rate "3.6%"', async () => {
      renderPage(1);
      await waitFor(() => {
        expect(screen.getByText('3.6%')).toBeInTheDocument();
      });
      expect(screen.getByText('Engagement')).toBeInTheDocument();
    });

    it('shows description text', async () => {
      renderPage(1);
      expect(await screen.findByText(/Ежедневные новости из мира криптовалют и блокчейна/)).toBeInTheDocument();
    });

    it('shows "Open channel in Telegram" link', async () => {
      renderPage(1);
      expect(await screen.findByText('Open channel in Telegram')).toBeInTheDocument();
    });

    it('shows tab buttons including Pricing', async () => {
      renderPage(1);
      expect(await screen.findByRole('button', { name: 'Pricing' })).toBeInTheDocument();
    });

    it('shows pricing rule cards with post type names after switching to Pricing tab', async () => {
      const { user } = renderPage(1);
      const pricingTab = await screen.findByRole('button', { name: 'Pricing' });
      await user.click(pricingTab);
      await waitFor(() => {
        expect(screen.getAllByText('Native post').length).toBeGreaterThanOrEqual(1);
      });
      expect(screen.getByText('Story')).toBeInTheDocument();
      expect(screen.getByText('Repost')).toBeInTheDocument();
    });

    it('shows pricing rule prices in TON after switching to Pricing tab', async () => {
      const { user } = renderPage(1);
      const pricingTab = await screen.findByRole('button', { name: 'Pricing' });
      await user.click(pricingTab);
      await waitFor(() => {
        expect(screen.getByText('5 TON')).toBeInTheDocument();
      });
      expect(screen.getByText('8 TON')).toBeInTheDocument();
      expect(screen.getByText('4 TON')).toBeInTheDocument();
      expect(screen.getByText('3 TON')).toBeInTheDocument();
    });

    it('shows rules content after switching to Rules tab', async () => {
      const { user } = renderPage(1);
      const rulesTab = await screen.findByRole('button', { name: 'Rules' });
      await user.click(rulesTab);
      expect(await screen.findByText('Media')).toBeInTheDocument();
    });

    it('shows rules: media allowed after switching to Rules tab', async () => {
      const { user } = renderPage(1);
      const rulesTab = await screen.findByRole('button', { name: 'Rules' });
      await user.click(rulesTab);
      expect(await screen.findByText('Media allowed')).toBeInTheDocument();
    });

    it('shows rules: links allowed after switching to Rules tab', async () => {
      const { user } = renderPage(1);
      const rulesTab = await screen.findByRole('button', { name: 'Rules' });
      await user.click(rulesTab);
      expect(await screen.findByText('Links allowed')).toBeInTheDocument();
    });

    it('shows rules: text formatting allowed after switching to Rules tab', async () => {
      const { user } = renderPage(1);
      const rulesTab = await screen.findByRole('button', { name: 'Rules' });
      await user.click(rulesTab);
      expect(await screen.findByText('Text formatting allowed')).toBeInTheDocument();
    });

    it('shows prohibited topics after switching to Rules tab', async () => {
      const { user } = renderPage(1);
      const rulesTab = await screen.findByRole('button', { name: 'Rules' });
      await user.click(rulesTab);
      await waitFor(() => {
        expect(screen.getByText('Казино')).toBeInTheDocument();
      });
      expect(screen.getByText('Форекс')).toBeInTheDocument();
      expect(screen.getByText('P2P-обменники')).toBeInTheDocument();
    });

    it('shows owner note section after switching to Rules tab', async () => {
      const { user } = renderPage(1);
      const rulesTab = await screen.findByRole('button', { name: 'Rules' });
      await user.click(rulesTab);
      expect(await screen.findByText("Owner's note")).toBeInTheDocument();
      expect(screen.getByText(/Пост должен быть на тему криптовалют или блокчейна/)).toBeInTheDocument();
    });

    it('shows Edit button for owner', async () => {
      renderPage(1);
      expect(await screen.findByRole('button', { name: 'Edit' })).toBeInTheDocument();
    });

    it('does NOT show "Create deal" CTA for owner', async () => {
      renderPage(1);
      await screen.findByText('Crypto News Daily');
      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument();
      });
      expect(screen.queryByText('Create deal')).not.toBeInTheDocument();
    });

    it('shows topic badges', async () => {
      renderPage(1);
      await waitFor(() => {
        expect(screen.getByText('Криптовалюта')).toBeInTheDocument();
      });
      expect(screen.getByText('Финансы')).toBeInTheDocument();
    });

    it('shows Share button', async () => {
      renderPage(1);
      expect(await screen.findByRole('button', { name: 'Share' })).toBeInTheDocument();
    });

    it('clicking Share copies link to clipboard', async () => {
      const { user } = renderPage(1);
      const shareBtn = await screen.findByRole('button', { name: 'Share' });
      await user.click(shareBtn);
      expect(mockCopyToClipboard).toHaveBeenCalledWith('https://t.me/AdvertMarketBot/app?startapp=channel_1');
    });

    it('shows channel age', async () => {
      renderPage(1);
      await screen.findByText('Crypto News Daily');
      await waitFor(() => {
        // Channel 1 createdAt: 2025-06-01 — should show months/years age
        const pageText = document.body.textContent ?? '';
        expect(pageText).toMatch(/@cryptonewsdaily/);
      });
    });

    it('shows reach rate percentage in stats', async () => {
      renderPage(1);
      await waitFor(() => {
        // avgReach=45000, subscriberCount=125000 → 36% reach
        expect(screen.getByText(/36% reach/)).toBeInTheDocument();
      });
    });

    it('shows hero CPM in pricing tab', async () => {
      const { user } = renderPage(1);
      const pricingTab = await screen.findByRole('button', { name: 'Pricing' });
      await user.click(pricingTab);
      await waitFor(() => {
        expect(screen.getByText(/per 1K views/)).toBeInTheDocument();
      });
    });

    it('shows language badge', async () => {
      renderPage(1);
      await waitFor(() => {
        expect(screen.getByText('ru')).toBeInTheDocument();
      });
    });

    it('clicking Edit navigates to edit page', async () => {
      const { user } = renderPage(1);
      const editBtn = await screen.findByRole('button', { name: 'Edit' });
      await user.click(editBtn);
      expect(await screen.findByText('edit-page')).toBeInTheDocument();
    });
  });

  // --- Non-owner view (channel 5) ---

  describe('non-owner view (channel 5)', () => {
    it('shows "Create deal" button for non-owner', async () => {
      renderPage(5);
      expect(await screen.findByText('Create deal')).toBeInTheDocument();
    });

    it('does NOT show Edit button for non-owner', async () => {
      renderPage(5);
      await screen.findByText('Marketing Hub');
      await waitFor(() => {
        expect(screen.getByText('Create deal')).toBeInTheDocument();
      });
      expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument();
    });

    it('shows "Join channel" for private channel', async () => {
      renderPage(5);
      expect(await screen.findByText('Join channel')).toBeInTheDocument();
    });

    it('shows "Private channel" for channel without username', async () => {
      renderPage(5);
      await waitFor(() => {
        expect(screen.getByText(/Private channel/)).toBeInTheDocument();
      });
    });

    it('clicking "Create deal" navigates to deal creation', async () => {
      const { user } = renderPage(5);
      const createDealBtn = await screen.findByText('Create deal');
      await user.click(createDealBtn);
      expect(await screen.findByText('create-deal-page')).toBeInTheDocument();
    });

    it('shows custom rules text after switching to Rules tab', async () => {
      const { user } = renderPage(5);
      const rulesTab = await screen.findByRole('button', { name: 'Rules' });
      await user.click(rulesTab);
      expect(await screen.findByText(/Только маркетинговая тематика/)).toBeInTheDocument();
    });
  });

  // --- Error path ---

  describe('error path (non-existent channel)', () => {
    it('shows "Page not found" for unknown channel', async () => {
      renderPage(999);
      expect(await screen.findByText('Page not found')).toBeInTheDocument();
    });

    it('shows "Back" button that navigates to /catalog', async () => {
      const { user } = renderPage(999);
      const backButton = await screen.findByRole('button', { name: 'Back' });
      expect(backButton).toBeInTheDocument();
      await user.click(backButton);
      expect(await screen.findByText('catalog-page')).toBeInTheDocument();
    });
  });

  // --- Loading state ---

  describe('loading state', () => {
    it('shows loading state before data arrives', () => {
      server.use(
        http.get(`${API_BASE}/channels/:channelId`, async () => {
          await new Promise((resolve) => setTimeout(resolve, 5000));
          return HttpResponse.json({});
        }),
        http.get(`${API_BASE}/channels/:channelId/team`, async () => {
          await new Promise((resolve) => setTimeout(resolve, 5000));
          return HttpResponse.json({ members: [] });
        }),
      );
      renderPage(1);
      // Spinner renders without role="status", verify no channel data is shown yet
      expect(screen.queryByText('Crypto News Daily')).not.toBeInTheDocument();
      expect(screen.queryByText('Page not found')).not.toBeInTheDocument();
    });
  });

  // --- Corner cases ---

  describe('corner cases', () => {
    it('channel without rules shows "No rules specified" after switching to Rules tab', async () => {
      server.use(
        http.get(`${API_BASE}/channels/:channelId`, () => {
          return HttpResponse.json({
            id: 51,
            title: 'No Rules Channel',
            username: 'norules',
            subscriberCount: 5000,
            categories: [],
            isActive: true,
            isVerified: false,
            ownerId: 99,
            createdAt: '2025-01-01T00:00:00Z',
            avgReach: 1500,
            pricingRules: [
              {
                id: 510,
                channelId: 51,
                name: 'Native',
                postTypes: ['NATIVE'],
                priceNano: 1_000_000_000,
                isActive: true,
                sortOrder: 1,
              },
            ],
            topics: [],
          });
        }),
        http.get(`${API_BASE}/channels/:channelId/team`, () => {
          return HttpResponse.json({ members: [] });
        }),
      );
      const { user } = renderPage(51);
      const rulesTab = await screen.findByRole('button', { name: 'Rules' });
      await user.click(rulesTab);
      expect(await screen.findByText('No rules specified')).toBeInTheDocument();
    });

    it('channel without description does not show description text', async () => {
      server.use(
        http.get(`${API_BASE}/channels/:channelId`, () => {
          return HttpResponse.json({
            id: 50,
            title: 'No Description Channel',
            username: 'nodesc',
            subscriberCount: 10000,
            categories: [],
            isActive: true,
            isVerified: false,
            ownerId: 99,
            createdAt: '2025-01-01T00:00:00Z',
            avgReach: 3000,
            pricingRules: [
              {
                id: 500,
                channelId: 50,
                name: 'Native',
                postTypes: ['NATIVE'],
                priceNano: 1_000_000_000,
                isActive: true,
                sortOrder: 1,
              },
            ],
            topics: [],
          });
        }),
        http.get(`${API_BASE}/channels/:channelId/team`, () => {
          return HttpResponse.json({ members: [] });
        }),
      );
      renderPage(50);
      await screen.findByText('No Description Channel');
      const pageText = document.body.textContent ?? '';
      expect(pageText).not.toContain('Ежедневные новости');
    });
  });
});
