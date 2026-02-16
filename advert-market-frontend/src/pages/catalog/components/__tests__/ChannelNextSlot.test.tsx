import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { ChannelNextSlot } from '../ChannelNextSlot';

vi.mock('@/shared/lib/time-utils', () => ({
  formatTimeUntil: vi.fn(),
}));

vi.mock('motion/react', () => ({
  motion: {
    div: ({ children, ...props }: React.HTMLAttributes<HTMLDivElement>) => <div {...props}>{children}</div>,
  },
}));

import { formatTimeUntil } from '@/shared/lib/time-utils';

const mockFormatTimeUntil = vi.mocked(formatTimeUntil);

describe('ChannelNextSlot', () => {
  it('renders time until next slot when slot is in the future', () => {
    mockFormatTimeUntil.mockReturnValue('2h 30m');

    renderWithProviders(<ChannelNextSlot nextAvailableSlot="2026-02-14T14:30:00Z" />);

    expect(screen.getByText(/2h 30m/)).toBeInTheDocument();
  });

  it('renders "Next slot in" text with time when slot is in the future', () => {
    mockFormatTimeUntil.mockReturnValue('5h 15m');

    renderWithProviders(<ChannelNextSlot nextAvailableSlot="2026-02-14T17:15:00Z" />);

    // en.json: "catalog.channel.nextSlot": "Next slot in {{time}}"
    expect(screen.getByText('Next slot in 5h 15m')).toBeInTheDocument();
  });

  it('renders "Slot available" when slot time has passed', () => {
    mockFormatTimeUntil.mockReturnValue(null);

    renderWithProviders(<ChannelNextSlot nextAvailableSlot="2026-02-12T10:00:00Z" />);

    // en.json: "catalog.channel.nextSlotAvailable": "Slot available"
    expect(screen.getByText('Slot available')).toBeInTheDocument();
  });

  it('renders clock icon', () => {
    mockFormatTimeUntil.mockReturnValue('1h 0m');

    const { container } = renderWithProviders(<ChannelNextSlot nextAvailableSlot="2026-02-14T13:00:00Z" />);

    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('calls formatTimeUntil with the provided slot date', () => {
    mockFormatTimeUntil.mockReturnValue('3h 45m');

    renderWithProviders(<ChannelNextSlot nextAvailableSlot="2026-02-14T15:45:00Z" />);

    expect(mockFormatTimeUntil).toHaveBeenCalledWith('2026-02-14T15:45:00Z');
  });

  it('uses section background when slot is in the future', () => {
    mockFormatTimeUntil.mockReturnValue('1h');

    const { container } = renderWithProviders(<ChannelNextSlot nextAvailableSlot="2026-02-14T13:00:00Z" />);

    const styledDiv = container.querySelector('.rounded-\\[10px\\]') as HTMLElement;
    expect(styledDiv).toBeTruthy();
    expect(styledDiv.className).toContain('bg-bg-secondary');
  });

  it('uses success background when slot is available now', () => {
    mockFormatTimeUntil.mockReturnValue(null);

    const { container } = renderWithProviders(<ChannelNextSlot nextAvailableSlot="2026-02-12T10:00:00Z" />);

    const styledDiv = container.querySelector('.rounded-\\[10px\\]') as HTMLElement;
    expect(styledDiv).toBeTruthy();
    expect(styledDiv.className).toContain('bg-soft-success');
  });
});
