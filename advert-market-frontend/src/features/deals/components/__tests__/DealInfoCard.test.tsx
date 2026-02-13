import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { Deal } from '../../types/deal';
import { DealInfoCard } from '../DealInfoCard';

vi.mock('@/shared/hooks/use-countdown', () => ({
  useCountdown: vi.fn(() => null),
}));

function makeDeal(overrides: Partial<Deal> = {}): Deal {
  return {
    id: 'deal-1',
    status: 'OFFER_PENDING',
    channelId: 1,
    channelTitle: 'Test Channel',
    channelUsername: 'testchannel',
    postType: 'NATIVE',
    priceNano: 5_000_000_000,
    durationHours: null,
    postFrequencyHours: null,
    role: 'ADVERTISER',
    advertiserId: 100,
    ownerId: 200,
    message: null,
    deadlineAt: null,
    createdAt: '2026-01-15T10:00:00Z',
    updatedAt: '2026-01-15T10:00:00Z',
    ...overrides,
  };
}

describe('DealInfoCard', () => {
  it('renders formatted TON price', () => {
    renderWithProviders(<DealInfoCard deal={makeDeal()} />);
    expect(screen.getByText('5 TON')).toBeInTheDocument();
  });

  it('renders fiat price estimate', () => {
    renderWithProviders(<DealInfoCard deal={makeDeal()} />);
    // 5 TON * 5.5 = $27.50
    expect(screen.getByText('~$27.50')).toBeInTheDocument();
  });

  it('renders post type chip', () => {
    renderWithProviders(<DealInfoCard deal={makeDeal({ postType: 'NATIVE' })} />);
    expect(screen.getByText('Native post')).toBeInTheDocument();
  });

  it('renders creation date chip', () => {
    renderWithProviders(<DealInfoCard deal={makeDeal({ createdAt: '2026-06-15T10:00:00Z' })} />);
    expect(screen.getByText(/Jun/)).toBeInTheDocument();
  });

  it('renders overlap chip when both freq and duration are present', () => {
    renderWithProviders(<DealInfoCard deal={makeDeal({ postFrequencyHours: 4, durationHours: 24 })} />);
    // overlapFormat: "{{freq}}/{{dur}}" => "4/24"
    expect(screen.getByText('4/24')).toBeInTheDocument();
  });

  it('renders only duration chip when freq is absent', () => {
    renderWithProviders(<DealInfoCard deal={makeDeal({ durationHours: 48 })} />);
    // onlyDuration: "{{dur}}h" => "48h"
    expect(screen.getByText('48h')).toBeInTheDocument();
  });

  it('does not render overlap chip when neither freq nor duration is present', () => {
    renderWithProviders(<DealInfoCard deal={makeDeal({ postFrequencyHours: null, durationHours: null })} />);
    expect(screen.queryByText(/\/\d+/)).not.toBeInTheDocument();
  });

  it('renders deal message when present', () => {
    renderWithProviders(<DealInfoCard deal={makeDeal({ message: 'Please post on Monday' })} />);
    expect(screen.getByText('Please post on Monday')).toBeInTheDocument();
  });

  it('does not render message block when message is null', () => {
    renderWithProviders(<DealInfoCard deal={makeDeal({ message: null })} />);
    expect(screen.queryByText(/Please post/)).not.toBeInTheDocument();
  });

  it('renders countdown chip when deadline is present', async () => {
    const { useCountdown } = await import('@/shared/hooks/use-countdown');
    vi.mocked(useCountdown).mockReturnValue('2d 5h');

    renderWithProviders(<DealInfoCard deal={makeDeal({ deadlineAt: '2026-02-20T00:00:00Z' })} />);
    expect(screen.getByText('2d 5h')).toBeInTheDocument();
  });
});
