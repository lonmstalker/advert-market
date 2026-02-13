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
    const separators = container.querySelectorAll('div > div');
    // wrapper > [separator, text, separator] — find divs with background
    const lines = Array.from(separators).filter((el) => (el as HTMLElement).style.height === '0.5px');
    expect(lines).toHaveLength(2);
  });
});
