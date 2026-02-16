vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
  isHapticFeedbackSupported: vi.fn(() => false),
}));

import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@/test/test-utils';
import type { DealStatus } from '../../types/deal';
import { MiniTimeline } from '../MiniTimeline';

describe('MiniTimeline', () => {
  it('renders 3 stage labels', () => {
    render(<MiniTimeline currentStatus="DRAFT" />);

    expect(screen.getByText('Agreement')).toBeDefined();
    expect(screen.getByText('Payment')).toBeDefined();
    expect(screen.getByText('Publication')).toBeDefined();
  });

  it('renders a check icon for completed stages', () => {
    render(<MiniTimeline currentStatus="SCHEDULED" />);

    const checks = screen.getAllByTestId('mini-timeline-check');
    expect(checks).toHaveLength(2);
  });

  it('renders a pulsing dot for active stage', () => {
    render(<MiniTimeline currentStatus="FUNDED" />);

    expect(screen.getByTestId('mini-timeline-active')).toBeDefined();
  });

  it('renders a pending dot for pending stages', () => {
    render(<MiniTimeline currentStatus="DRAFT" />);

    const pending = screen.getAllByTestId('mini-timeline-pending');
    expect(pending).toHaveLength(2);
  });

  it('renders all checks when completed', () => {
    render(<MiniTimeline currentStatus="COMPLETED_RELEASED" />);

    const checks = screen.getAllByTestId('mini-timeline-check');
    expect(checks).toHaveLength(3);
    expect(screen.queryByTestId('mini-timeline-active')).toBeNull();
    expect(screen.queryByTestId('mini-timeline-pending')).toBeNull();
  });

  it('handles all terminal statuses with all completed', () => {
    const terminals: DealStatus[] = ['DISPUTED', 'CANCELLED', 'EXPIRED', 'REFUNDED', 'PARTIALLY_REFUNDED'];
    for (const status of terminals) {
      const { unmount } = render(<MiniTimeline currentStatus={status} />);
      const checks = screen.getAllByTestId('mini-timeline-check');
      expect(checks).toHaveLength(3);
      unmount();
    }
  });

  it('renders connecting lines between nodes', () => {
    render(<MiniTimeline currentStatus="FUNDED" />);

    const lines = screen.getAllByTestId('mini-timeline-line');
    expect(lines).toHaveLength(2);
  });
});
