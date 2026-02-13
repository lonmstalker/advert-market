import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { ChannelDetail } from '../../types/channel';
import { ChannelStats } from '../ChannelStats';

const baseChannel: ChannelDetail = {
  id: 1,
  title: 'Test Channel',
  subscriberCount: 125000,
  categories: [],
  isActive: true,
  ownerId: 10,
  createdAt: '2025-01-01T00:00:00Z',
  pricingRules: [],
  topics: [],
  avgReach: 45000,
  engagementRate: 3.6,
};

function renderStats(overrides: Partial<ChannelDetail> = {}) {
  const channel: ChannelDetail = { ...baseChannel, ...overrides };
  return renderWithProviders(<ChannelStats channel={channel} />);
}

describe('ChannelStats', () => {
  it('renders the statistics group header', () => {
    renderStats();
    expect(screen.getByText('Statistics')).toBeInTheDocument();
  });

  it('renders subscriber count formatted with locale', () => {
    renderStats({ subscriberCount: 125000 });
    // formatLocaleNumber uses ru-RU locale: 125 000 (with non-breaking space)
    expect(screen.getByText('Subscribers')).toBeInTheDocument();
    expect(screen.getByText(/125/)).toBeInTheDocument();
  });

  it('renders avg reach when present', () => {
    renderStats({ avgReach: 45000 });
    expect(screen.getByText('Avg. reach')).toBeInTheDocument();
    expect(screen.getByText(/45/)).toBeInTheDocument();
  });

  it('does not render avg reach when null', () => {
    renderStats({ avgReach: undefined });
    expect(screen.queryByText('Avg. reach')).not.toBeInTheDocument();
  });

  it('renders engagement rate with one decimal and percent sign', () => {
    renderStats({ engagementRate: 3.6 });
    expect(screen.getByText('Engagement')).toBeInTheDocument();
    expect(screen.getByText('3.6%')).toBeInTheDocument();
  });

  it('does not render engagement rate when null', () => {
    renderStats({ engagementRate: undefined });
    expect(screen.queryByText('Engagement')).not.toBeInTheDocument();
  });
});
