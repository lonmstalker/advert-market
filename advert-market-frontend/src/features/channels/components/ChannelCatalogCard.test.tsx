import { renderWithProviders, screen } from '@/test/test-utils';
import type { Channel } from '../types/channel';
import { ChannelCatalogCard } from './ChannelCatalogCard';

const baseChannel: Channel = {
  id: 1,
  title: 'Crypto News Daily',
  username: 'cryptonewsdaily',
  subscriberCount: 125000,
  categories: ['crypto', 'finance'],
  pricePerPostNano: 5_000_000_000,
  avgViews: 45000,
  engagementRate: 3.6,
  isActive: true,
  isVerified: true,
  language: 'ru',
};

function renderCard(channelOverrides: Partial<Channel> = {}, onClick = vi.fn()) {
  const channel: Channel = { ...baseChannel, ...channelOverrides };
  const result = renderWithProviders(<ChannelCatalogCard channel={channel} onClick={onClick} />);
  return { ...result, onClick };
}

describe('ChannelCatalogCard', () => {
  it('renders channel title', () => {
    renderCard();
    expect(screen.getByText('Crypto News Daily')).toBeInTheDocument();
  });

  it('shows verified icon when isVerified is true', () => {
    renderCard({ isVerified: true });
    expect(screen.getByRole('img', { name: /verified/i })).toBeInTheDocument();
  });

  it('does not show verified icon when isVerified is false', () => {
    renderCard({ isVerified: false });
    expect(screen.queryByRole('img', { name: /verified/i })).not.toBeInTheDocument();
  });

  it('shows @username in subtitle when present', () => {
    renderCard({ username: 'cryptonewsdaily' });
    expect(screen.getByText(/@cryptonewsdaily/)).toBeInTheDocument();
  });

  it('does not show @username when missing', () => {
    renderCard({ username: undefined });
    expect(screen.queryByText(/@/)).not.toBeInTheDocument();
  });

  it('shows price when pricePerPostNano is set', () => {
    renderCard({ pricePerPostNano: 5_000_000_000 });
    expect(screen.getByText(/from.*5 TON/)).toBeInTheDocument();
  });

  it('does not show price section when pricePerPostNano is undefined', () => {
    renderCard({ pricePerPostNano: undefined });
    expect(screen.queryByText(/from .* TON/)).not.toBeInTheDocument();
  });

  it('shows subscriber count in subtitle formatted as 125K', () => {
    renderCard({ subscriberCount: 125000 });
    expect(screen.getByText(/125K.*subs/)).toBeInTheDocument();
  });

  it('calls onClick when card is clicked', async () => {
    const onClick = vi.fn();
    const { user } = renderCard({}, onClick);
    await user.click(screen.getByText('Crypto News Daily'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('shows language badge for channel.language', () => {
    renderCard({ language: 'ru', languages: undefined });
    expect(screen.getByText('ru')).toBeInTheDocument();
  });

  it('formats large subscriber count as 1.5M', () => {
    renderCard({ subscriberCount: 1_500_000 });
    expect(screen.getByText(/1\.5M/)).toBeInTheDocument();
  });

  it('formats small subscriber count as raw number', () => {
    renderCard({ subscriberCount: 500 });
    expect(screen.getByText(/500/)).toBeInTheDocument();
  });
});
