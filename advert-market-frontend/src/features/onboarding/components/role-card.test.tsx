import { describe, expect, it, vi } from 'vitest';
import { render, screen, userEvent } from '@/test/test-utils';
import { RoleCard } from './role-card';

describe('RoleCard', () => {
  it('shows explicit selected marker', () => {
    render(
      <RoleCard
        icon={<span data-testid="icon">*</span>}
        title="Advertiser"
        hint="Run campaigns"
        isSelected
        onToggle={vi.fn()}
      />,
    );

    const root = screen.getByTestId('role-card-root');
    expect(root.querySelector('svg')).toBeTruthy();
  });

  it('calls toggle on click', async () => {
    const onToggle = vi.fn();
    render(
      <RoleCard
        icon={<span data-testid="icon">*</span>}
        title="Advertiser"
        hint="Run campaigns"
        isSelected={false}
        onToggle={onToggle}
      />,
    );

    await userEvent.click(screen.getByTestId('role-card-trigger'));
    expect(onToggle).toHaveBeenCalledTimes(1);
  });
});
