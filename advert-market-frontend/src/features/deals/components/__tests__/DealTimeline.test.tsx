import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { TimelineStep } from '../../lib/deal-status';
import { DealTimeline } from '../DealTimeline';

function makeStep(overrides: Partial<TimelineStep> & Pick<TimelineStep, 'status' | 'state' | 'label'>): TimelineStep {
  return { ...overrides };
}

const completedStep = (label: string, timestamp: string): TimelineStep =>
  makeStep({ status: 'DRAFT', state: 'completed', label, timestamp });

const activeStep = (label: string, description?: string): TimelineStep =>
  makeStep({ status: 'OFFER_PENDING', state: 'active', label, description });

const pendingStep = (label: string, status = 'NEGOTIATING' as TimelineStep['status']): TimelineStep =>
  makeStep({ status, state: 'pending', label });

describe('DealTimeline', () => {
  it('renders the "Timeline" heading', () => {
    renderWithProviders(<DealTimeline steps={[activeStep('Offer Pending')]} />);
    expect(screen.getByText('Timeline')).toBeInTheDocument();
  });

  it('renders all non-pending and visible pending steps', () => {
    const steps: TimelineStep[] = [
      completedStep('Draft', '2026-01-01T00:00:00Z'),
      activeStep('Offer Pending'),
      pendingStep('Negotiating'),
      pendingStep('Accepted', 'ACCEPTED'),
    ];
    renderWithProviders(<DealTimeline steps={steps} />);
    expect(screen.getByText('Draft')).toBeInTheDocument();
    expect(screen.getByText('Offer Pending')).toBeInTheDocument();
    expect(screen.getByText('Negotiating')).toBeInTheDocument();
    expect(screen.getByText('Accepted')).toBeInTheDocument();
  });

  it('renders the timeline as an accessible list', () => {
    renderWithProviders(<DealTimeline steps={[activeStep('Offer Pending')]} />);
    expect(screen.getByRole('list', { name: 'Timeline' })).toBeInTheDocument();
  });

  it('shows formatted date for completed steps with timestamp', () => {
    const steps: TimelineStep[] = [completedStep('Draft', '2026-06-15T12:00:00Z')];
    renderWithProviders(<DealTimeline steps={steps} />);
    // Intl.DateTimeFormat with { day: 'numeric', month: 'short' } -> "Jun 15" or similar
    expect(screen.getByText(/Jun/)).toBeInTheDocument();
  });

  it('collapses pending steps beyond VISIBLE_PENDING limit and shows expand button', () => {
    const steps: TimelineStep[] = [
      activeStep('Current step'),
      pendingStep('Pending 1', 'NEGOTIATING'),
      pendingStep('Pending 2', 'ACCEPTED'),
      pendingStep('Pending 3', 'AWAITING_PAYMENT'),
      pendingStep('Pending 4', 'FUNDED'),
    ];
    renderWithProviders(<DealTimeline steps={steps} />);
    // Only first 2 pending steps visible
    expect(screen.getByText('Pending 1')).toBeInTheDocument();
    expect(screen.getByText('Pending 2')).toBeInTheDocument();
    // Remaining pending steps should be hidden
    expect(screen.queryByText('Pending 3')).not.toBeInTheDocument();
    expect(screen.queryByText('Pending 4')).not.toBeInTheDocument();
    // Expand button should appear with collapsed count
    expect(screen.getByText(/more step/)).toBeInTheDocument();
  });

  it('reveals all pending steps when expand button is clicked', async () => {
    const steps: TimelineStep[] = [
      activeStep('Current step'),
      pendingStep('Pending 1', 'NEGOTIATING'),
      pendingStep('Pending 2', 'ACCEPTED'),
      pendingStep('Pending 3', 'AWAITING_PAYMENT'),
      pendingStep('Pending 4', 'FUNDED'),
    ];
    const { user } = renderWithProviders(<DealTimeline steps={steps} />);
    await user.click(screen.getByText(/more step/));
    expect(screen.getByText('Pending 3')).toBeInTheDocument();
    expect(screen.getByText('Pending 4')).toBeInTheDocument();
  });

  it('does not show expand button when pending steps fit within limit', () => {
    const steps: TimelineStep[] = [
      completedStep('Draft', '2026-01-01T00:00:00Z'),
      activeStep('Offer Pending'),
      pendingStep('Negotiating'),
    ];
    renderWithProviders(<DealTimeline steps={steps} />);
    expect(screen.queryByText(/more step/)).not.toBeInTheDocument();
  });

  it('expands description when clicking an active step with description', async () => {
    const steps: TimelineStep[] = [activeStep('Offer Pending', 'Waiting for owner to respond')];
    const { user } = renderWithProviders(<DealTimeline steps={steps} />);
    await user.click(screen.getByText('Offer Pending'));
    expect(screen.getByText('Waiting for owner to respond')).toBeInTheDocument();
  });
});
