import { describe, expect, it, vi } from 'vitest';
import { render, screen, userEvent } from '@/test/test-utils';
import { Chip } from '../chip';

describe('Chip', () => {
  it('uses am-chip class and active modifier when active', () => {
    render(<Chip label="Crypto" active onClick={vi.fn()} />);
    const chip = screen.getByRole('button', { name: 'Crypto' });
    expect(chip).toHaveClass('am-chip');
    expect(chip).toHaveClass('am-chip--active');
  });

  it('does not have active modifier when inactive', () => {
    render(<Chip label="Crypto" active={false} onClick={vi.fn()} />);
    const chip = screen.getByRole('button', { name: 'Crypto' });
    expect(chip).not.toHaveClass('am-chip--active');
  });

  it('calls onClick', async () => {
    const onClick = vi.fn();
    render(<Chip label="Crypto" active={false} onClick={onClick} />);
    await userEvent.click(screen.getByRole('button', { name: 'Crypto' }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('applies rounded variant class', () => {
    render(<Chip label="Tag" active={false} onClick={vi.fn()} variant="rounded" />);
    const chip = screen.getByRole('button', { name: 'Tag' });
    expect(chip).toHaveClass('am-chip--rounded');
  });
});
