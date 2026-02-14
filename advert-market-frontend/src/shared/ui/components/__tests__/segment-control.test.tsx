import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SegmentControl } from '../segment-control';

const tabs = [
  { value: 'tab1', label: 'First' },
  { value: 'tab2', label: 'Second' },
  { value: 'tab3', label: 'Third' },
] as const;

describe('SegmentControl', () => {
  it('renders all tab labels', () => {
    render(<SegmentControl tabs={[...tabs]} active="tab1" onChange={vi.fn()} />);
    expect(screen.getByText('First')).toBeInTheDocument();
    expect(screen.getByText('Second')).toBeInTheDocument();
    expect(screen.getByText('Third')).toBeInTheDocument();
  });

  it('renders the correct number of buttons', () => {
    render(<SegmentControl tabs={[...tabs]} active="tab1" onChange={vi.fn()} />);
    expect(screen.getAllByRole('button')).toHaveLength(3);
  });

  it('applies active styles to the selected tab', () => {
    render(<SegmentControl tabs={[...tabs]} active="tab2" onChange={vi.fn()} />);
    const activeButton = screen.getByText('Second');
    expect(activeButton.style.fontWeight).toBe('500');
    expect(activeButton.style.color).toBe('var(--color-static-white)');
  });

  it('applies inactive styles to non-selected tabs', () => {
    render(<SegmentControl tabs={[...tabs]} active="tab2" onChange={vi.fn()} />);
    const inactiveButton = screen.getByText('First');
    expect(inactiveButton.style.fontWeight).toBe('500');
    expect(inactiveButton.style.background).toBe('transparent');
  });

  it('calls onChange with the tab value when a tab is clicked', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<SegmentControl tabs={[...tabs]} active="tab1" onChange={onChange} />);
    await user.click(screen.getByText('Third'));
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith('tab3');
  });

  it('does not crash with a single tab', () => {
    const singleTab = [{ value: 'only', label: 'Only Tab' }];
    render(<SegmentControl tabs={singleTab} active="only" onChange={vi.fn()} />);
    expect(screen.getByText('Only Tab')).toBeInTheDocument();
    expect(screen.getAllByRole('button')).toHaveLength(1);
  });
});
