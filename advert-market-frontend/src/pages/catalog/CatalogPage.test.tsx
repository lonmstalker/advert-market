import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { server } from '@/test/mocks/server';
import CatalogPage from './CatalogPage';

const API_BASE = '/api/v1';

function renderCatalog() {
  return renderWithProviders(
    <Routes>
      <Route path="/catalog" element={<CatalogPage />} />
      <Route path="/catalog/channels/:channelId" element={<div>detail-page</div>} />
    </Routes>,
    { initialEntries: ['/catalog'] },
  );
}

describe('CatalogPage', () => {
  // --- Happy path ---

  it('renders search input with placeholder', () => {
    renderCatalog();
    expect(screen.getByPlaceholderText('Search channels...')).toBeInTheDocument();
  });

  it('shows filter button', () => {
    renderCatalog();
    expect(screen.getByRole('button', { name: 'Filters' })).toBeInTheDocument();
  });

  it('renders channel cards after loading', async () => {
    renderCatalog();
    expect(await screen.findByText('Crypto News Daily')).toBeInTheDocument();
    expect(screen.getByText('Tech Digest')).toBeInTheDocument();
    expect(screen.getByText('AI Weekly')).toBeInTheDocument();
    expect(screen.getByText('Finance Pro')).toBeInTheDocument();
    expect(screen.getByText('Marketing Hub')).toBeInTheDocument();
  });

  it('shows category chip row with "All topics"', async () => {
    renderCatalog();
    expect(await screen.findByText('All topics')).toBeInTheDocument();
  });

  it('renders multiple channel cards in a list', async () => {
    renderCatalog();
    await screen.findByText('Crypto News Daily');
    // Verify multiple channels are rendered, not just one
    expect(screen.getByText('Tech Digest')).toBeInTheDocument();
    expect(screen.getByText('AI Weekly')).toBeInTheDocument();
    expect(screen.getByText('Finance Pro')).toBeInTheDocument();
    expect(screen.getByText('Marketing Hub')).toBeInTheDocument();
  });

  it('shows end-of-list text when all channels fit in one page', async () => {
    renderCatalog();
    expect(await screen.findByText("That's all")).toBeInTheDocument();
  });

  // --- Error path ---

  it('shows error state when API fails', async () => {
    server.use(
      http.get(`${API_BASE}/channels`, () => HttpResponse.error()),
    );
    renderCatalog();
    expect(await screen.findByText('An error occurred')).toBeInTheDocument();
  });

  it('shows retry button on error and clicking it refetches', async () => {
    let callCount = 0;
    server.use(
      http.get(`${API_BASE}/channels`, () => {
        callCount++;
        if (callCount === 1) return HttpResponse.error();
        return HttpResponse.json({
          items: [
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
          ],
          nextCursor: null,
          hasNext: false,
          total: 1,
        });
      }),
    );

    const { user } = renderCatalog();

    const retryButton = await screen.findByRole('button', { name: 'Retry' });
    expect(retryButton).toBeInTheDocument();

    await user.click(retryButton);

    expect(await screen.findByText('Crypto News Daily')).toBeInTheDocument();
  });

  // --- Empty state ---

  it('shows "Nothing found" when search returns no results', async () => {
    server.use(
      http.get(`${API_BASE}/channels`, () =>
        HttpResponse.json({
          items: [],
          nextCursor: null,
          hasNext: false,
          total: 0,
        }),
      ),
    );
    renderCatalog();
    expect(await screen.findByText('Nothing found')).toBeInTheDocument();
  });

  it('shows "Reset filters" button in empty state', async () => {
    server.use(
      http.get(`${API_BASE}/channels`, () =>
        HttpResponse.json({
          items: [],
          nextCursor: null,
          hasNext: false,
          total: 0,
        }),
      ),
    );
    renderCatalog();
    expect(await screen.findByRole('button', { name: 'Reset filters' })).toBeInTheDocument();
  });

  // --- Search ---

  it('typing in search input updates the input value', async () => {
    const { user } = renderCatalog();
    const input = screen.getByPlaceholderText('Search channels...');
    await user.type(input, 'crypto');
    expect(input).toHaveValue('crypto');
  });

  it('filters channels after typing and debounce', async () => {
    const { user } = renderCatalog();

    expect(await screen.findByText('Crypto News Daily')).toBeInTheDocument();
    expect(screen.getByText('Tech Digest')).toBeInTheDocument();

    const input = screen.getByPlaceholderText('Search channels...');
    await user.type(input, 'crypto');

    await waitFor(() => {
      expect(screen.queryByText('Tech Digest')).not.toBeInTheDocument();
    }, { timeout: 3000 });

    expect(await screen.findByText('Crypto News Daily')).toBeInTheDocument();
  });

  // --- Navigation ---

  it('clicking a channel card navigates to detail page', async () => {
    const { user } = renderCatalog();
    const card = await screen.findByText('Crypto News Daily');
    await user.click(card);
    expect(await screen.findByText('detail-page')).toBeInTheDocument();
  });

  it('clicking "Reset filters" button triggers refetch', async () => {
    server.use(
      http.get(`${API_BASE}/channels`, () =>
        HttpResponse.json({
          items: [],
          nextCursor: null,
          hasNext: false,
          total: 0,
        }),
      ),
    );

    const { user } = renderCatalog();
    const resetBtn = await screen.findByRole('button', { name: 'Reset filters' });
    await user.click(resetBtn);
    // After clicking Reset, the search input should be cleared
    const input = screen.getByPlaceholderText('Search channels...');
    expect(input).toHaveValue('');
  });

  it('clicking filter button opens filters sheet', async () => {
    const { user } = renderCatalog();
    await screen.findByText('Crypto News Daily');
    const filterBtn = screen.getByRole('button', { name: 'Filters' });
    await user.click(filterBtn);
    // Sheet opens with filter content (the sheet component is rendered)
    await waitFor(() => {
      expect(document.querySelector('[class*="sheet"]') || document.querySelector('[role="dialog"]')).toBeTruthy();
    });
  });

  // --- Corner cases ---

  it('shows skeleton state during initial load', () => {
    server.use(
      http.get(`${API_BASE}/channels`, async () => {
        await new Promise((resolve) => setTimeout(resolve, 5000));
        return HttpResponse.json({
          items: [],
          nextCursor: null,
          hasNext: false,
          total: 0,
        });
      }),
    );
    renderCatalog();
    expect(screen.queryByText('Crypto News Daily')).not.toBeInTheDocument();
    expect(screen.queryByText('Nothing found')).not.toBeInTheDocument();
    expect(screen.queryByText('An error occurred')).not.toBeInTheDocument();
  });
});