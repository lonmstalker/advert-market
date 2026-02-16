import { describe, expect, it, vi } from 'vitest';
import { render, screen, userEvent } from '@/test/test-utils';
import { Chip } from '../chip';

describe('Chip', () => {
  it('uses tinted active style by default', () => {
    render(<Chip label="Crypto" active onClick={vi.fn()} />);
    const chip = screen.getByRole('button', { name: 'Crypto' });
    expect(chip).toHaveStyle({ background: 'var(--am-soft-accent-bg)' });
    expect(chip).toHaveStyle({ color: 'var(--color-accent-primary)' });
  });

  it('uses solid active style when tone is solid', () => {
    render(<Chip label="Crypto" active tone="solid" onClick={vi.fn()} />);
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
