import { Route, Routes } from 'react-router';
import { act, renderWithProviders, screen, waitFor } from '@/test/test-utils';
import RegisterChannelPage from '../RegisterChannelPage';

const mockVerifyChannel = vi.fn();
const mockRegisterChannel = vi.fn();
const mockCreateChannelPricingRule = vi.fn();
const mockUpdateChannel = vi.fn();
const backClickIfAvailable = vi.fn();

vi.mock('@/features/channels', () => ({
  fetchCategories: vi.fn(async () => []),
  fetchPostTypes: vi.fn(async () => [
    { type: 'NATIVE', labels: { en: 'Native', ru: 'Нативная реклама' } },
    { type: 'STORY', labels: { en: 'Story', ru: 'Сторис' } },
  ]),
  verifyChannel: (...args: unknown[]) => mockVerifyChannel(...args),
  registerChannel: (...args: unknown[]) => mockRegisterChannel(...args),
  createChannelPricingRule: (...args: unknown[]) => mockCreateChannelPricingRule(...args),
  updateChannel: (...args: unknown[]) => mockUpdateChannel(...args),
}));

vi.mock('@/shared/hooks/use-haptic', () => ({
  useHaptic: () => ({
    selectionChanged: vi.fn(),
    impactOccurred: vi.fn(),
    notificationOccurred: vi.fn(),
  }),
}));

vi.mock('@/shared/hooks/use-toast', () => ({
  useToast: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showInfo: vi.fn(),
  }),
}));

let backHandler: (() => void) | null = null;

function wrapIfAvailable<TArgs extends unknown[]>(fn: (...args: TArgs) => unknown) {
  return {
    ifAvailable: (...args: TArgs) => [true, fn(...args)] as const,
  };
}

vi.mock('@telegram-apps/sdk-react', () => ({
  showBackButton: wrapIfAvailable(vi.fn()),
  onBackButtonClick: wrapIfAvailable((handler: () => void) => {
    backHandler = handler;
    backClickIfAvailable(handler);
  }),
  offBackButtonClick: wrapIfAvailable(vi.fn()),
  openLink: wrapIfAvailable(vi.fn(() => false)),
  openTelegramLink: wrapIfAvailable(vi.fn(() => false)),
}));

describe('RegisterChannelPage', () => {
  beforeEach(() => {
    backHandler = null;
    backClickIfAvailable.mockReset();
    mockVerifyChannel.mockReset();
    mockRegisterChannel.mockReset();
    mockCreateChannelPricingRule.mockReset();
    mockUpdateChannel.mockReset();
    mockVerifyChannel.mockResolvedValue({
      channelId: 100001,
      username: 'mynewchannel',
      title: 'My New Channel',
      subscriberCount: 500,
      botStatus: { isAdmin: true },
    });
    mockRegisterChannel.mockResolvedValue({
      id: 100001,
      title: 'My New Channel',
      username: 'mynewchannel',
      subscriberCount: 500,
      categories: [],
      isActive: true,
      ownerId: 1,
      createdAt: '2026-02-17T00:00:00Z',
    });
    mockCreateChannelPricingRule.mockResolvedValue({
      id: 1,
      channelId: 100001,
      name: 'Native',
      description: null,
      postTypes: ['NATIVE'],
      priceNano: 3_000_000_000,
      isActive: true,
      sortOrder: 0,
    });
    mockUpdateChannel.mockResolvedValue({
      id: 100001,
      title: 'My New Channel',
      username: 'mynewchannel',
      subscriberCount: 500,
      categories: [],
      isActive: true,
      ownerId: 1,
      createdAt: '2026-02-17T00:00:00Z',
    });
  });

  function renderPage() {
    return renderWithProviders(
      <Routes>
        <Route path="/profile/channels/new" element={<RegisterChannelPage />} />
        <Route path="/profile" element={<div>profile-page</div>} />
      </Routes>,
      { initialEntries: ['/profile/channels/new'] },
    );
  }

  it('navigates to profile on Telegram back from step 1', async () => {
    renderPage();

    await waitFor(() => {
      expect(backClickIfAvailable).toHaveBeenCalled();
      expect(backHandler).not.toBeNull();
    });

    act(() => {
      backHandler?.();
    });

    await waitFor(() => {
      expect(screen.getByText('profile-page')).toBeInTheDocument();
    });
  });

  it('returns from step 2 to step 1 on Telegram back', async () => {
    const { user } = renderPage();

    await waitFor(() => expect(backHandler).not.toBeNull());

    await user.type(screen.getByRole('textbox'), 'mynewchannel');
    await user.click(screen.getByRole('button', { name: 'Verify' }));

    await waitFor(() => {
      expect(screen.getByText('My New Channel')).toBeInTheDocument();
    });

    act(() => {
      backHandler?.();
    });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Verify' })).toBeInTheDocument();
    });
  });

  it('shows inline error for unsupported invite links without calling API', async () => {
    const { user } = renderPage();

    await user.type(screen.getByRole('textbox'), 'https://t.me/+invite');
    await user.click(screen.getByRole('button', { name: 'Verify' }));

    expect(mockVerifyChannel).not.toHaveBeenCalled();
    expect(
      screen.getByText('Invite links are not supported. Use @username, t.me/c/... link, or numeric channel ID.'),
    ).toBeInTheDocument();
  });

  it('shows owner note and pricing rules fields on setup step', async () => {
    const { user } = renderPage();

    await user.type(screen.getByRole('textbox'), 'mynewchannel');
    await user.click(screen.getByRole('button', { name: 'Verify' }));

    expect(await screen.findByText('My New Channel')).toBeInTheDocument();
    expect(screen.getByText('Owner note')).toBeInTheDocument();
    expect(screen.getByText('Placement rules')).toBeInTheDocument();
  });

  it('submits setup as channel register + pricing rules + owner note update', async () => {
    const { user } = renderPage();

    await user.type(screen.getByRole('textbox'), 'mynewchannel');
    await user.click(screen.getByRole('button', { name: 'Verify' }));
    expect(await screen.findByText('My New Channel')).toBeInTheDocument();

    const ownerNoteField = screen.getByPlaceholderText('What should advertisers know before making a deal?');
    await user.type(ownerNoteField, 'Only crypto-related ads');

    const priceField = screen.getByPlaceholderText('0.00');
    await user.type(priceField, '3');

    await user.click(screen.getByRole('button', { name: 'Register' }));

    await waitFor(() => {
      expect(mockRegisterChannel).toHaveBeenCalled();
      expect(mockCreateChannelPricingRule).toHaveBeenCalled();
      expect(mockUpdateChannel).toHaveBeenCalledWith(
        100001,
        expect.objectContaining({
          customRules: 'Only crypto-related ads',
        }),
      );
    });
  });
});
