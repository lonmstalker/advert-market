import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { EndOfList } from '../end-of-list';

describe('EndOfList', () => {
  it('renders the label text', () => {
    render(<EndOfList label="No more items" />);
    expect(screen.getByText('No more items')).toBeInTheDocument();
  });

  it('renders two separator lines', () => {
    const { container } = render(<EndOfList label="End" />);
    const lines = container.querySelectorAll('.bg-separator');
    expect(lines).toHaveLength(2);
  });
});
