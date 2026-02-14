import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { QuickActions } from '../QuickActions';

const mockImpactOccurred = vi.fn();

vi.mock('@/shared/hooks/use-haptic', () => ({
  useHaptic: () => ({
    impactOccurred: mockImpactOccurred,
    notificationOccurred: vi.fn(),
    selectionChanged: vi.fn(),
  }),
}));

describe('QuickActions', () => {
  it('renders 3 action buttons', () => {
    renderWithProviders(<QuickActions />);
    expect(screen.getByText('Top up')).toBeInTheDocument();
    expect(screen.getByText('Withdraw')).toBeInTheDocument();
    expect(screen.getByText('Transfer')).toBeInTheDocument();
  });

  it('has proper aria-labels for accessibility', () => {
    renderWithProviders(<QuickActions />);
    expect(screen.getByRole('button', { name: 'Top up' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Withdraw' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Transfer' })).toBeInTheDocument();
  });

  it('triggers haptic feedback on click', async () => {
    const { user } = renderWithProviders(<QuickActions />);
    await user.click(screen.getByRole('button', { name: 'Top up' }));
    expect(mockImpactOccurred).toHaveBeenCalledWith('light');
  });
});
