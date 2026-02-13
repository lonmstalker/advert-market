import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { FormattedPrice } from '../formatted-price';

vi.mock('@/shared/lib/ton-format', () => ({
  formatTon: (nano: number) => `${(nano / 1_000_000_000).toFixed(2)} TON`,
}));

vi.mock('@/shared/lib/fiat-format', () => ({
  formatFiat: (nano: number) => `~$${((nano / 1_000_000_000) * 5.5).toFixed(2)}`,
}));

describe('FormattedPrice', () => {
  it('renders TON price', () => {
    render(<FormattedPrice nanoTon={1_000_000_000} />);
    expect(screen.getByText('1.00 TON')).toBeInTheDocument();
  });

  it('renders fiat price by default', () => {
    render(<FormattedPrice nanoTon={1_000_000_000} />);
    expect(screen.getByText('~$5.50')).toBeInTheDocument();
  });

  it('hides fiat price when showFiat is false', () => {
    render(<FormattedPrice nanoTon={1_000_000_000} showFiat={false} />);
    expect(screen.getByText('1.00 TON')).toBeInTheDocument();
    expect(screen.queryByText('~$5.50')).not.toBeInTheDocument();
  });

  it('renders with sm size', () => {
    const { container } = render(<FormattedPrice nanoTon={2_000_000_000} size="sm" />);
    expect(screen.getByText('2.00 TON')).toBeInTheDocument();
    expect(screen.getByText('~$11.00')).toBeInTheDocument();
    // Both TON and fiat should have tabular-nums
    const spans = container.querySelectorAll('span[style*="tabular-nums"]');
    expect(spans.length).toBe(2);
  });

  it('renders with md size (default)', () => {
    render(<FormattedPrice nanoTon={500_000_000} />);
    expect(screen.getByText('0.50 TON')).toBeInTheDocument();
    expect(screen.getByText('~$2.75')).toBeInTheDocument();
  });

  it('renders with lg size', () => {
    render(<FormattedPrice nanoTon={10_000_000_000} size="lg" />);
    expect(screen.getByText('10.00 TON')).toBeInTheDocument();
    expect(screen.getByText('~$55.00')).toBeInTheDocument();
  });

  it('applies tabular-nums style to TON price', () => {
    const { container } = render(<FormattedPrice nanoTon={1_000_000_000} showFiat={false} />);
    const tonSpan = container.querySelector('span[style*="tabular-nums"]');
    expect(tonSpan).toBeInTheDocument();
    expect(tonSpan?.textContent).toBe('1.00 TON');
  });

  it('applies tabular-nums style to fiat price', () => {
    const { container } = render(<FormattedPrice nanoTon={1_000_000_000} />);
    const spans = container.querySelectorAll('span[style*="tabular-nums"]');
    expect(spans.length).toBe(2);
    expect(spans[1]?.textContent).toBe('~$5.50');
  });

  it('renders zero nanoTon', () => {
    render(<FormattedPrice nanoTon={0} />);
    expect(screen.getByText('0.00 TON')).toBeInTheDocument();
    expect(screen.getByText('~$0.00')).toBeInTheDocument();
  });

  it('renders large nanoTon values', () => {
    render(<FormattedPrice nanoTon={100_000_000_000} />);
    expect(screen.getByText('100.00 TON')).toBeInTheDocument();
    expect(screen.getByText('~$550.00')).toBeInTheDocument();
  });
});
