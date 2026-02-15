import { retrieveLaunchParams } from '@telegram-apps/sdk-react';
import { Route, Routes } from 'react-router';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { DeepLinkHandler } from './deep-link-handler';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveLaunchParams: vi.fn(),
}));

function TestRoutes() {
  return (
    <Routes>
      <Route path="*" element={<DeepLinkHandler />} />
      <Route path="/catalog/channels/:channelId" element={<div>channel-page</div>} />
      <Route path="/deals/:id" element={<div>deal-page</div>} />
    </Routes>
  );
}

describe('DeepLinkHandler', () => {
  afterEach(() => vi.resetAllMocks());

  it('navigates to /catalog/:id for channel_ start_param', async () => {
    vi.mocked(retrieveLaunchParams).mockReturnValue({ tgWebAppStartParam: 'channel_123' } as never);
    renderWithProviders(<TestRoutes />, { initialEntries: ['/'] });
    expect(await screen.findByText('channel-page')).toBeInTheDocument();
  });

  it('navigates to /deals/:id for deal_ start_param', async () => {
    vi.mocked(retrieveLaunchParams).mockReturnValue({ tgWebAppStartParam: 'deal_456' } as never);
    renderWithProviders(<TestRoutes />, { initialEntries: ['/'] });
    expect(await screen.findByText('deal-page')).toBeInTheDocument();
  });

  it('does not navigate without start_param', () => {
    vi.mocked(retrieveLaunchParams).mockReturnValue({} as never);
    renderWithProviders(<TestRoutes />, { initialEntries: ['/'] });
    expect(screen.queryByText('channel-page')).not.toBeInTheDocument();
    expect(screen.queryByText('deal-page')).not.toBeInTheDocument();
  });

  it('does not throw when launch params are not available', () => {
    vi.mocked(retrieveLaunchParams).mockImplementation(() => {
      throw new Error('no launch params');
    });
    expect(() => {
      renderWithProviders(<TestRoutes />, { initialEntries: ['/'] });
    }).not.toThrow();
  });
});
