vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@/test/test-utils';

import { VerifiedBadge } from '../verified-badge';

describe('VerifiedBadge', () => {
  it('renders SVG element', () => {
    const { container } = render(<VerifiedBadge />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('default size is 16', () => {
    const { container } = render(<VerifiedBadge />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '16');
    expect(svg).toHaveAttribute('height', '16');
  });

  it('custom size applies to width and height', () => {
    const { container } = render(<VerifiedBadge size={24} />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '24');
    expect(svg).toHaveAttribute('height', '24');
  });

  it('has aria-label="Verified"', () => {
    render(<VerifiedBadge />);
    const svg = screen.getByLabelText('Verified');
    expect(svg).toBeInTheDocument();
  });

  it('applies className', () => {
    const { container } = render(<VerifiedBadge className="custom-class" />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveClass('custom-class');
  });
});
