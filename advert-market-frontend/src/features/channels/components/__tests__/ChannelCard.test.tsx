import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { Channel, ChannelDetail } from '../../types/channel';
import { ChannelCard } from '../ChannelCard';

const baseChannel: Channel = {
  id: 1,
  title: 'Crypto News Daily',
  username: 'cryptonewsdaily',
  subscriberCount: 125000,
  categories: ['crypto'],
  isActive: true,
};

function renderCard(overrides: Partial<Channel> = {}) {
  const channel: Channel = { ...baseChannel, ...overrides };
  return renderWithProviders(<ChannelCard channel={channel} />);
}

describe('ChannelCard', () => {
  it('renders channel title', () => {
    renderCard();
    expect(screen.getByText('Crypto News Daily')).toBeInTheDocument();
  });

  it('renders subscriber count formatted compactly as 125K', () => {
    renderCard({ subscriberCount: 125000 });
    expect(screen.getByText(/125K/)).toBeInTheDocument();
  });

  it('renders subscriber count as 1.5M for millions', () => {
    renderCard({ subscriberCount: 1_500_000 });
    expect(screen.getByText(/1\.5M/)).toBeInTheDocument();
  });

  it('renders small subscriber count as raw number', () => {
    renderCard({ subscriberCount: 500 });
    expect(screen.getByText(/500/)).toBeInTheDocument();
  });

  it('renders subscribers label', () => {
    renderCard();
    expect(screen.getByText(/subscribers/)).toBeInTheDocument();
  });

  it('renders avatar with first letter of title', () => {
    renderCard({ title: 'Alpha Channel' });
    expect(screen.getByText('A')).toBeInTheDocument();
  });

  it('works with ChannelDetail type', () => {
    const detail: ChannelDetail = {
      ...baseChannel,
      ownerId: 10,
      createdAt: '2025-01-01T00:00:00Z',
      pricingRules: [],
      topics: [],
    };
    renderWithProviders(<ChannelCard channel={detail} />);
    expect(screen.getByText('Crypto News Daily')).toBeInTheDocument();
  });
});
