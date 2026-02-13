import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Popover } from '../popover';

describe('Popover', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders the trigger children', () => {
    render(
      <Popover content={<span>Tooltip text</span>}>
        <span>Trigger</span>
      </Popover>,
    );
    expect(screen.getByText('Trigger')).toBeInTheDocument();
  });

  it('does not show popover content initially', () => {
    render(
      <Popover content={<span>Tooltip text</span>}>
        <span>Trigger</span>
      </Popover>,
    );
    expect(screen.queryByText('Tooltip text')).not.toBeInTheDocument();
  });

  it('shows popover content after clicking the trigger', async () => {
    const user = userEvent.setup();
    render(
      <Popover content={<span>Tooltip text</span>}>
        <span>Trigger</span>
      </Popover>,
    );
    await user.click(screen.getByRole('button'));
    expect(screen.getByText('Tooltip text')).toBeInTheDocument();
  });

  it('sets open state to false when clicking trigger again (toggle)', async () => {
    const user = userEvent.setup();
    render(
      <Popover content={<span>Tooltip text</span>}>
        <span>Trigger</span>
      </Popover>,
    );
    const trigger = screen.getByRole('button');

    // Open the popover
    await user.click(trigger);
    expect(screen.getByText('Tooltip text')).toBeInTheDocument();

    // Toggle off -- AnimatePresence may keep it momentarily during exit animation
    await user.click(trigger);
    await waitFor(() => {
      expect(screen.queryByText('Tooltip text')).not.toBeInTheDocument();
    });
  });

  it('renders the trigger as a button element', () => {
    render(
      <Popover content={<span>Info</span>}>
        <span>Icon</span>
      </Popover>,
    );
    const button = screen.getByRole('button');
    expect(button).toBeInTheDocument();
    expect(button).toHaveAttribute('type', 'button');
  });
});
