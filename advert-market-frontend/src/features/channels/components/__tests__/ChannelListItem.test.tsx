import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { Channel } from '../../types/channel';
import { ChannelListItem } from '../ChannelListItem';

const baseChannel: Channel = {
  id: 1,
  title: 'Tech Digest',
  username: 'techdigest',
  subscriberCount: 50000,
  categories: ['tech'],
  pricePerPostNano: 2_000_000_000,
  isActive: true,
};

function renderListItem(overrides: Partial<Channel> = {}, onClick = vi.fn()) {
  const channel: Channel = { ...baseChannel, ...overrides };
  const result = renderWithProviders(<ChannelListItem channel={channel} onClick={onClick} />);
  return { ...result, onClick };
}

describe('ChannelListItem', () => {
  it('renders channel title', () => {
    renderListItem();
    expect(screen.getByText('Tech Digest')).toBeInTheDocument();
  });

  it('renders subscriber count formatted compactly as 50K', () => {
    renderListItem({ subscriberCount: 50000 });
    expect(screen.getByText(/50K/)).toBeInTheDocument();
  });

  it('renders price in TON when pricePerPostNano is set', () => {
    renderListItem({ pricePerPostNano: 2_000_000_000 });
    expect(screen.getByText('2 TON')).toBeInTheDocument();
  });

  it('does not render price when pricePerPostNano is undefined', () => {
    renderListItem({ pricePerPostNano: undefined });
    expect(screen.queryByText(/TON/)).not.toBeInTheDocument();
  });

  it('renders avatar with first letter of title', () => {
    renderListItem({ title: 'Marketing Hub' });
    expect(screen.getByText('M')).toBeInTheDocument();
  });

  it('calls onClick handler when item is clicked', async () => {
    const onClick = vi.fn();
    const { user } = renderListItem({}, onClick);
    await user.click(screen.getByText('Tech Digest'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('renders subscribers label text', () => {
    renderListItem();
    expect(screen.getByText(/subs/)).toBeInTheDocument();
  });
});
