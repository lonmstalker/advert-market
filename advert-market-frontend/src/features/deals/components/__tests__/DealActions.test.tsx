import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import type { DealAction, DealActionType } from '../../lib/deal-actions';
import { DealActions } from '../DealActions';

function makeAction(type: DealActionType, overrides: Partial<DealAction> = {}): DealAction {
  const defaults: Record<DealActionType, Omit<DealAction, 'type'>> = {
    accept: { i18nKey: 'deals.actions.accept', variant: 'primary', requiresConfirm: false },
    reject: { i18nKey: 'deals.actions.reject', variant: 'destructive', requiresConfirm: true },
    cancel: { i18nKey: 'deals.actions.cancel', variant: 'destructive', requiresConfirm: true },
    counter_offer: { i18nKey: 'deals.actions.counterOffer', variant: 'secondary', requiresConfirm: false },
    reply: { i18nKey: 'deals.actions.reply', variant: 'primary', requiresConfirm: false },
    pay: { i18nKey: 'deals.actions.pay', variant: 'primary', requiresConfirm: false },
    approve_creative: { i18nKey: 'deals.actions.approveCreative', variant: 'primary', requiresConfirm: false },
    request_revision: { i18nKey: 'deals.actions.requestRevision', variant: 'secondary', requiresConfirm: false },
    publish: { i18nKey: 'deals.actions.publish', variant: 'primary', requiresConfirm: false },
    schedule: { i18nKey: 'deals.actions.schedule', variant: 'secondary', requiresConfirm: false },
  };
  return { type, ...defaults[type], ...overrides };
}

describe('DealActions', () => {
  it('returns null when actions array is empty', () => {
    const { container } = renderWithProviders(<DealActions actions={[]} onAction={vi.fn()} isPending={false} />);
    expect(container.innerHTML).toBe('');
  });

  it('renders a button for each action', () => {
    const actions = [makeAction('accept'), makeAction('counter_offer'), makeAction('reject')];
    renderWithProviders(<DealActions actions={actions} onAction={vi.fn()} isPending={false} />);
    expect(screen.getByText('Accept')).toBeInTheDocument();
    expect(screen.getByText('Counter-offer')).toBeInTheDocument();
    expect(screen.getByText('Reject')).toBeInTheDocument();
  });

  it('calls onAction directly for non-confirm actions', async () => {
    const onAction = vi.fn();
    const { user } = renderWithProviders(
      <DealActions actions={[makeAction('accept')]} onAction={onAction} isPending={false} />,
    );
    await user.click(screen.getByText('Accept'));
    expect(onAction).toHaveBeenCalledWith('accept');
    expect(onAction).toHaveBeenCalledTimes(1);
  });

  it('opens confirmation dialog for actions that require confirm', async () => {
    const onAction = vi.fn();
    const { user } = renderWithProviders(
      <DealActions actions={[makeAction('reject')]} onAction={onAction} isPending={false} />,
    );
    await user.click(screen.getByText('Reject'));
    // onAction should NOT be called yet
    expect(onAction).not.toHaveBeenCalled();
    // Confirmation dialog should appear
    expect(screen.getByText('Are you sure?')).toBeInTheDocument();
  });

  it('calls onAction after confirming destructive action', async () => {
    const onAction = vi.fn();
    const { user } = renderWithProviders(
      <DealActions actions={[makeAction('reject')]} onAction={onAction} isPending={false} />,
    );
    // Open confirmation dialog
    await user.click(screen.getByText('Reject'));
    // The confirm dialog shows the action button text as confirm
    const confirmButtons = screen.getAllByText('Reject');
    // Click the confirm button in the dialog (second instance)
    await user.click(confirmButtons[confirmButtons.length - 1]);
    expect(onAction).toHaveBeenCalledWith('reject');
  });

  it('closes confirmation dialog on cancel without calling onAction', async () => {
    const onAction = vi.fn();
    const { user } = renderWithProviders(
      <DealActions actions={[makeAction('reject')]} onAction={onAction} isPending={false} />,
    );
    // Open the confirmation dialog
    await user.click(screen.getByText('Reject'));
    expect(screen.getByText('Are you sure?')).toBeInTheDocument();
    // Click the "Cancel" close button in the dialog (closeText = common.cancel = "Cancel")
    await user.click(screen.getByText('Cancel'));
    expect(onAction).not.toHaveBeenCalled();
  });

  it('renders pay button for AWAITING_PAYMENT advertiser', () => {
    renderWithProviders(<DealActions actions={[makeAction('pay')]} onAction={vi.fn()} isPending={false} />);
    expect(screen.getByText('Pay')).toBeInTheDocument();
  });

  it('renders approve + revision buttons for CREATIVE_SUBMITTED', () => {
    const actions = [makeAction('approve_creative'), makeAction('request_revision')];
    renderWithProviders(<DealActions actions={actions} onAction={vi.fn()} isPending={false} />);
    expect(screen.getByText('Approve')).toBeInTheDocument();
    expect(screen.getByText('Revision')).toBeInTheDocument();
  });

  it('renders publish + schedule buttons for CREATIVE_APPROVED owner', () => {
    const actions = [makeAction('publish'), makeAction('schedule')];
    renderWithProviders(<DealActions actions={actions} onAction={vi.fn()} isPending={false} />);
    expect(screen.getByText('Publish')).toBeInTheDocument();
    expect(screen.getByText('Schedule')).toBeInTheDocument();
  });

  it('calls onAction with correct type for pay action', async () => {
    const onAction = vi.fn();
    const { user } = renderWithProviders(
      <DealActions actions={[makeAction('pay')]} onAction={onAction} isPending={false} />,
    );
    await user.click(screen.getByText('Pay'));
    expect(onAction).toHaveBeenCalledWith('pay');
  });

  it('renders reply button for NEGOTIATING status', () => {
    const actions = [makeAction('reply'), makeAction('cancel')];
    renderWithProviders(<DealActions actions={actions} onAction={vi.fn()} isPending={false} />);
    expect(screen.getByText('Reply')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  it('shows confirmation description specific to cancel action', async () => {
    const { user } = renderWithProviders(
      <DealActions actions={[makeAction('cancel')]} onAction={vi.fn()} isPending={false} />,
    );
    await user.click(screen.getByText('Cancel'));
    expect(screen.getByText('This deal will be cancelled. This action cannot be undone.')).toBeInTheDocument();
  });
});
