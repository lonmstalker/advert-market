import { Route, Routes } from 'react-router';
import { installTelegramMock, removeTelegramMock } from '@/test/mocks/telegram';
import { renderWithProviders, screen } from '@/test/test-utils';
import { DeepLinkHandler } from './deep-link-handler';

function TestRoutes() {
  return (
    <Routes>
      <Route path="*" element={<DeepLinkHandler />} />
      <Route path="/catalog/:id" element={<div>channel-page</div>} />
      <Route path="/deals/:id" element={<div>deal-page</div>} />
    </Routes>
  );
}

describe('DeepLinkHandler', () => {
  afterEach(() => {
    removeTelegramMock();
  });

  it('navigates to /catalog/:id for channel_ start_param', async () => {
    installTelegramMock({
      initDataUnsafe: { start_param: 'channel_123' },
    });
    renderWithProviders(<TestRoutes />, { initialEntries: ['/'] });
    expect(await screen.findByText('channel-page')).toBeInTheDocument();
  });

  it('navigates to /deals/:id for deal_ start_param', async () => {
    installTelegramMock({
      initDataUnsafe: { start_param: 'deal_456' },
    });
    renderWithProviders(<TestRoutes />, { initialEntries: ['/'] });
    expect(await screen.findByText('deal-page')).toBeInTheDocument();
  });

  it('does not navigate without start_param', () => {
    installTelegramMock();
    renderWithProviders(<TestRoutes />, { initialEntries: ['/'] });
    expect(screen.queryByText('channel-page')).not.toBeInTheDocument();
    expect(screen.queryByText('deal-page')).not.toBeInTheDocument();
  });

  it('does not throw when window.Telegram is not available', () => {
    expect(() => {
      renderWithProviders(<TestRoutes />, { initialEntries: ['/'] });
    }).not.toThrow();
  });
});
