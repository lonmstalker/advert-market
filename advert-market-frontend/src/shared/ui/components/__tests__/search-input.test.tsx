import { describe, expect, it, vi } from 'vitest';
import { render, screen, userEvent } from '@/test/test-utils';
import { SearchInput } from '../search-input';

vi.mock('@/shared/hooks/use-haptic', () => ({
  useHaptic: () => ({
    impactOccurred: vi.fn(),
    notificationOccurred: vi.fn(),
    selectionChanged: vi.fn(),
  }),
}));

describe('SearchInput', () => {
  it('calls onChange when input changes', async () => {
    const onChange = vi.fn();
    render(<SearchInput value="" onChange={onChange} placeholder="Search" />);
    await userEvent.type(screen.getByRole('searchbox', { name: 'Search' }), 'ton');
    expect(onChange).toHaveBeenCalled();
  });
});
