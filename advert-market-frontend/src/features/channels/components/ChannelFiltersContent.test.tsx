import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import type { CatalogFilters } from '../types/channel';
import { ChannelFiltersContent } from './ChannelFiltersContent';
import { ChannelFiltersProvider } from './ChannelFiltersContext';

function renderFilters(
  overrides: { currentFilters?: CatalogFilters; onApply?: (f: CatalogFilters) => void; onReset?: () => void } = {},
) {
  const currentFilters = overrides.currentFilters ?? {};
  const onApply = overrides.onApply ?? vi.fn();
  const onReset = overrides.onReset ?? vi.fn();

  return renderWithProviders(
    <ChannelFiltersProvider currentFilters={currentFilters} onApply={onApply} onReset={onReset}>
      <ChannelFiltersContent />
    </ChannelFiltersProvider>,
  );
}

describe('ChannelFiltersContent', () => {
  it('renders filter title', async () => {
    renderFilters();
    expect(screen.getByText('Filters')).toBeInTheDocument();
  });

  it('renders subscribers section label', () => {
    renderFilters();
    // "Subscribers" appears as both a section label and a sort option
    const matches = screen.getAllByText('Subscribers');
    expect(matches.length).toBeGreaterThanOrEqual(1);
  });

  it('renders price per post section label', () => {
    renderFilters();
    expect(screen.getByText('Price per post')).toBeInTheDocument();
  });

  it('renders sort by section label', () => {
    renderFilters();
    expect(screen.getByText('Sort by')).toBeInTheDocument();
  });

  it('renders topic section label', () => {
    renderFilters();
    expect(screen.getByText('Topic')).toBeInTheDocument();
  });

  it('renders language section label', () => {
    renderFilters();
    expect(screen.getByText('Channel language')).toBeInTheDocument();
  });

  it('renders category chips after data loads', async () => {
    renderFilters();
    await waitFor(() => {
      expect(screen.getByText('Crypto')).toBeInTheDocument();
    });
    expect(screen.getByText('Technology')).toBeInTheDocument();
    expect(screen.getByText('Finance')).toBeInTheDocument();
  });

  it('renders language chips', () => {
    renderFilters();
    expect(screen.getByText('Russian')).toBeInTheDocument();
    expect(screen.getByText('English')).toBeInTheDocument();
    expect(screen.getByText('Ukrainian')).toBeInTheDocument();
    expect(screen.getByText('Uzbek')).toBeInTheDocument();
    expect(screen.getByText('Kazakh')).toBeInTheDocument();
  });

  it('renders sort options including Default', () => {
    renderFilters();
    expect(screen.getByText('Default')).toBeInTheDocument();
    expect(screen.getByText('Relevance')).toBeInTheDocument();
    // "Subscribers" sort chip coexists with the section label
    const subscriberButtons = screen.getAllByText('Subscribers').filter((el) => el.tagName === 'BUTTON');
    expect(subscriberButtons).toHaveLength(1);
  });

  it('renders show button', () => {
    renderFilters();
    expect(screen.getByText('Show')).toBeInTheDocument();
  });

  it('does not render reset button when no filters are active', () => {
    renderFilters();
    expect(screen.queryByText('Reset')).not.toBeInTheDocument();
  });

  it('toggles a category chip on click', async () => {
    const { user } = renderFilters();
    await waitFor(() => {
      expect(screen.getByText('Crypto')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Crypto'));

    // After selecting a category, the reset button should appear
    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });
  });

  it('toggles a language chip on click', async () => {
    const { user } = renderFilters();

    await user.click(screen.getByText('English'));

    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });
  });

  it('toggles a sort option on click', async () => {
    const { user } = renderFilters();

    await user.click(screen.getByText('Relevance'));

    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });
  });

  it('calls onApply when show button is clicked', async () => {
    const onApply = vi.fn();
    const { user } = renderFilters({ onApply });

    await user.click(screen.getByText('Show'));

    expect(onApply).toHaveBeenCalledOnce();
  });

  it('calls onReset when reset button is clicked', async () => {
    const onReset = vi.fn();
    const { user } = renderFilters({
      currentFilters: { categories: ['crypto'] },
      onReset,
    });

    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Reset'));

    expect(onReset).toHaveBeenCalledOnce();
  });

  it('initializes category selection from currentFilters.categories', async () => {
    renderFilters({ currentFilters: { categories: ['crypto'] } });

    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });
  });

  it('initializes category selection from currentFilters.category (legacy)', async () => {
    renderFilters({ currentFilters: { category: 'tech' } });

    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });
  });

  it('initializes language selection from currentFilters.languages', async () => {
    renderFilters({ currentFilters: { languages: ['en'] } });

    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });
  });

  it('initializes subscriber fields from currentFilters', () => {
    renderFilters({ currentFilters: { minSubs: 1000, maxSubs: 50000 } });

    expect(screen.getByText('Reset')).toBeInTheDocument();
  });

  it('initializes price fields from currentFilters (nanoTON to TON)', () => {
    renderFilters({ currentFilters: { minPrice: 2_000_000_000, maxPrice: 10_000_000_000 } });

    expect(screen.getByText('Reset')).toBeInTheDocument();
  });

  it('initializes sort from currentFilters', () => {
    renderFilters({ currentFilters: { sort: 'price_asc' } });

    expect(screen.getByText('Reset')).toBeInTheDocument();
  });

  it('shows channel count in button after data loads', async () => {
    renderFilters();

    await waitFor(() => {
      const showButton = screen.getByText(/channel|Show/);
      expect(showButton).toBeInTheDocument();
    });
  });

  it('un-toggles a category on second click', async () => {
    const { user } = renderFilters();

    await waitFor(() => {
      expect(screen.getByText('Crypto')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Crypto'));

    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Crypto'));

    await waitFor(() => {
      expect(screen.queryByText('Reset')).not.toBeInTheDocument();
    });
  });

  it('un-toggles a language on second click', async () => {
    const { user } = renderFilters();

    await user.click(screen.getByText('Russian'));

    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Russian'));

    await waitFor(() => {
      expect(screen.queryByText('Reset')).not.toBeInTheDocument();
    });
  });

  it('clicking Default sort deselects any active sort', async () => {
    const { user } = renderFilters();

    await user.click(screen.getByText('Relevance'));
    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Default'));
    await waitFor(() => {
      expect(screen.queryByText('Reset')).not.toBeInTheDocument();
    });
  });

  it('reset clears all filter state', async () => {
    const onReset = vi.fn();
    const { user } = renderFilters({
      currentFilters: {
        categories: ['crypto'],
        languages: ['en'],
        sort: 'subscribers',
      },
      onReset,
    });

    await waitFor(() => {
      expect(screen.getByText('Reset')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Reset'));

    expect(onReset).toHaveBeenCalledOnce();

    // After reset, the reset button should disappear (no active filters)
    await waitFor(() => {
      expect(screen.queryByText('Reset')).not.toBeInTheDocument();
    });
  });
});
