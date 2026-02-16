import { Route, Routes } from 'react-router';
import { act, renderWithProviders, screen, waitFor } from '@/test/test-utils';
import RegisterChannelPage from '../RegisterChannelPage';

const mockVerifyChannel = vi.fn();
const backClickIfAvailable = vi.fn();

vi.mock('@/features/channels', () => ({
  fetchCategories: vi.fn(async () => []),
  verifyChannel: (...args: unknown[]) => mockVerifyChannel(...args),
  registerChannel: vi.fn(),
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
    mockVerifyChannel.mockResolvedValue({
      channelId: 'ch-1',
      username: 'mynewchannel',
      title: 'My New Channel',
      subscriberCount: 500,
      botStatus: { isAdmin: true },
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

    await user.type(screen.getByPlaceholderText('@username'), 'mynewchannel');
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
});
