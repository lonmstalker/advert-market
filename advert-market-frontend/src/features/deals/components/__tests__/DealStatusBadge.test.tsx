import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { DEAL_STATUSES } from '../../types/deal';
import { DealStatusBadge } from '../DealStatusBadge';

describe('DealStatusBadge', () => {
  it('renders translated label for DRAFT status', () => {
    renderWithProviders(<DealStatusBadge status="DRAFT" />);
    expect(screen.getByText('Draft')).toBeInTheDocument();
  });

  it('renders translated label for OFFER_PENDING status', () => {
    renderWithProviders(<DealStatusBadge status="OFFER_PENDING" />);
    expect(screen.getByText('Offer Pending')).toBeInTheDocument();
  });

  it('renders translated label for AWAITING_PAYMENT status', () => {
    renderWithProviders(<DealStatusBadge status="AWAITING_PAYMENT" />);
    expect(screen.getByText('Awaiting Payment')).toBeInTheDocument();
  });

  it('renders translated label for COMPLETED_RELEASED status', () => {
    renderWithProviders(<DealStatusBadge status="COMPLETED_RELEASED" />);
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('renders translated label for DISPUTED status', () => {
    renderWithProviders(<DealStatusBadge status="DISPUTED" />);
    expect(screen.getByText('Disputed')).toBeInTheDocument();
  });

  it('renders translated label for CANCELLED status', () => {
    renderWithProviders(<DealStatusBadge status="CANCELLED" />);
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
  });

  it('renders translated label for PARTIALLY_REFUNDED status', () => {
    renderWithProviders(<DealStatusBadge status="PARTIALLY_REFUNDED" />);
    expect(screen.getByText('Partially Refunded')).toBeInTheDocument();
  });

  it('renders a badge for every known DealStatus without throwing', () => {
    for (const status of DEAL_STATUSES) {
      const { unmount } = renderWithProviders(<DealStatusBadge status={status} />);
      // The translated text should appear somewhere in the document
      expect(screen.getByText(/.+/)).toBeInTheDocument();
      unmount();
    }
  });

  it('applies background color style based on status color config', () => {
    const { container } = renderWithProviders(<DealStatusBadge status="DISPUTED" />);
    const badge = container.querySelector('span');
    expect(badge).toBeTruthy();
    // destructive color -> rgba(255, 59, 48, 0.1)
    expect(badge?.style.backgroundColor).toContain('rgba(255, 59, 48, 0.1)');
  });
});
