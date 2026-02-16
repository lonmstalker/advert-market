import { mockCategories } from '@/test/mocks/data';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { CategoryChipRow } from './CategoryChipRow';

vi.mock('@/shared/hooks/use-haptic', () => ({
  useHaptic: () => ({
    impactOccurred: vi.fn(),
    notificationOccurred: vi.fn(),
    selectionChanged: vi.fn(),
  }),
}));

describe('CategoryChipRow', () => {
  it('renders "All topics" chip when no selection', async () => {
    const onSelect = vi.fn();
    renderWithProviders(<CategoryChipRow selected={[]} onSelect={onSelect} />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'All topics' })).toBeInTheDocument();
    });
  });

  it('"All topics" chip is active when selected is empty', async () => {
    const onSelect = vi.fn();
    renderWithProviders(<CategoryChipRow selected={[]} onSelect={onSelect} />);

    const allTopicsChip = await screen.findByRole('button', { name: 'All topics' });
    expect(allTopicsChip).toHaveAttribute('data-active', 'true');
    expect(allTopicsChip).toHaveClass('am-chip');
  });

  it('renders category chips after data loads', async () => {
    const onSelect = vi.fn();
    renderWithProviders(<CategoryChipRow selected={[]} onSelect={onSelect} />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Crypto' })).toBeInTheDocument();
    });

    const buttons = screen.getAllByRole('button');
    expect(buttons).toHaveLength(mockCategories.length + 1);
  });

  it('clicking a category chip calls onSelect with [slug]', async () => {
    const onSelect = vi.fn();
    const { user } = renderWithProviders(<CategoryChipRow selected={[]} onSelect={onSelect} />);

    const cryptoChip = await screen.findByRole('button', { name: 'Crypto' });
    await user.click(cryptoChip);

    expect(onSelect).toHaveBeenCalledOnce();
    expect(onSelect).toHaveBeenCalledWith(['crypto']);
  });

  it('clicking an already-selected category calls onSelect without that slug', async () => {
    const onSelect = vi.fn();
    const { user } = renderWithProviders(<CategoryChipRow selected={['crypto', 'tech']} onSelect={onSelect} />);

    const cryptoChip = await screen.findByRole('button', { name: 'Crypto' });
    await user.click(cryptoChip);

    expect(onSelect).toHaveBeenCalledOnce();
    expect(onSelect).toHaveBeenCalledWith(['tech']);
  });

  it('clicking "All topics" calls onSelect with empty array', async () => {
    const onSelect = vi.fn();
    const { user } = renderWithProviders(<CategoryChipRow selected={['crypto']} onSelect={onSelect} />);

    const allTopicsChip = await screen.findByRole('button', { name: 'All topics' });
    await user.click(allTopicsChip);

    expect(onSelect).toHaveBeenCalledOnce();
    expect(onSelect).toHaveBeenCalledWith([]);
  });

  it('returns null initially before categories load', () => {
    const onSelect = vi.fn();
    const { container } = renderWithProviders(<CategoryChipRow selected={[]} onSelect={onSelect} />);

    expect(container.innerHTML).toBe('');
  });
});
