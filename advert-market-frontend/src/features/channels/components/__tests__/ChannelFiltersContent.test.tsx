import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import type { CatalogFilters, Category } from '../../types/channel';
import { ChannelFiltersContent } from '../ChannelFiltersContent';
import { ChannelFiltersProvider } from '../ChannelFiltersContext';

const MOCK_CATEGORIES: Category[] = [
  { slug: 'tech', sortOrder: 1, localizedName: { en: 'Technology', ru: 'Технологии' } },
  { slug: 'crypto', sortOrder: 2, localizedName: { en: 'Crypto', ru: 'Крипто' } },
];

vi.mock('../../api/channels', () => ({
  fetchCategories: vi.fn(() => Promise.resolve(MOCK_CATEGORIES)),
  fetchChannelCount: vi.fn(() => Promise.resolve(42)),
}));

function renderFilters(currentFilters: CatalogFilters = {}, onApply = vi.fn(), onReset = vi.fn()) {
  return renderWithProviders(
    <ChannelFiltersProvider currentFilters={currentFilters} onApply={onApply} onReset={onReset}>
      <ChannelFiltersContent />
    </ChannelFiltersProvider>,
  );
}

describe('ChannelFiltersContent', () => {
  it('renders the filters title', async () => {
    renderFilters();
    await waitFor(() => {
      expect(screen.getByText('Filters')).toBeInTheDocument();
    });
  });

  it('renders category chips after data loads', async () => {
    renderFilters();
    expect(await screen.findByText('Technology')).toBeInTheDocument();
    expect(screen.getByText('Crypto')).toBeInTheDocument();
  });

  it('renders language filter chips with full names', async () => {
    renderFilters();
    expect(await screen.findByText('Russian')).toBeInTheDocument();
    expect(screen.getByText('English')).toBeInTheDocument();
  });

  it('toggles a category chip on click', async () => {
    const { user } = renderFilters();
    const chip = await screen.findByText('Technology');
    await user.click(chip);
    expect(chip.closest('button')).toHaveAttribute('aria-pressed', 'true');
  });

  it('toggles a language chip on click', async () => {
    const { user } = renderFilters();
    const chip = await screen.findByText('English');
    await user.click(chip);
    expect(chip.closest('button')).toHaveAttribute('aria-pressed', 'true');
  });

  it('shows reset button only when filters are active', async () => {
    const { user } = renderFilters();
    expect(screen.queryByText('Reset')).not.toBeInTheDocument();

    const chip = await screen.findByText('Technology');
    await user.click(chip);
    expect(screen.getByText('Reset')).toBeInTheDocument();
  });

  it('calls onReset and clears selections when reset is clicked', async () => {
    const onReset = vi.fn();
    const { user } = renderFilters({}, vi.fn(), onReset);

    const chip = await screen.findByText('Technology');
    await user.click(chip);
    expect(chip.closest('button')).toHaveAttribute('aria-pressed', 'true');

    await user.click(screen.getByText('Reset'));
    expect(onReset).toHaveBeenCalledOnce();
    expect(chip.closest('button')).toHaveAttribute('aria-pressed', 'false');
  });

  it('calls onApply with draft filters when apply is clicked', async () => {
    const onApply = vi.fn();
    const { user } = renderFilters({}, onApply);

    const showButton = await screen.findByText(/42 channels/i);
    await user.click(showButton);
    expect(onApply).toHaveBeenCalledOnce();
  });

  it('renders chip groups with fieldset and aria-label', async () => {
    renderFilters();
    await screen.findByText('Technology');

    const groups = screen.getAllByRole('group');
    expect(groups.length).toBeGreaterThanOrEqual(3);
    expect(groups[0]).toHaveAttribute('aria-label', 'Topic');
    expect(groups[1]).toHaveAttribute('aria-label', 'Channel language');
    expect(groups[2]).toHaveAttribute('aria-label', 'Sort by');
  });

  it('initializes from currentFilters with pre-selected category', async () => {
    renderFilters({ categories: ['tech'] });
    const chip = await screen.findByText('Technology');
    expect(chip.closest('button')).toHaveAttribute('aria-pressed', 'true');
  });
});
