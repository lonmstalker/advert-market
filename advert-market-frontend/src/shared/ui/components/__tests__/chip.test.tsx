import { describe, expect, it, vi } from 'vitest';
import { render, screen, userEvent } from '@/test/test-utils';
import { Chip } from '../chip';

describe('Chip', () => {
  it('uses accent active style', () => {
    render(<Chip label="Crypto" active onClick={vi.fn()} />);
    const chip = screen.getByRole('button', { name: 'Crypto' });
    expect(chip).toHaveStyle({ background: 'var(--color-accent-primary)' });
    expect(chip).toHaveStyle({ color: 'var(--color-static-white)' });
  });

  it('calls onClick', async () => {
    const onClick = vi.fn();
    render(<Chip label="Crypto" active={false} onClick={onClick} />);
    await userEvent.click(screen.getByRole('button', { name: 'Crypto' }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
