import { describe, expect, it } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { ChannelHeader } from './channel-header';
import { MockupContainer } from './mockup-container';
import { MockupTextButton } from './mockup-text-button';
import { TaskHint } from './task-hint';

describe('Onboarding layout contract', () => {
  it('renders mockup container via class contract without inline style', () => {
    renderWithProviders(<MockupContainer>content</MockupContainer>);
    const container = screen.getByTestId('onboarding-mockup-container');
    expect(container.className).toContain('am-onboarding-mockup');
    expect(container).not.toHaveAttribute('style');
  });

  it('renders channel header via class contract without inline style', () => {
    renderWithProviders(<ChannelHeader icon={<span data-testid="icon">i</span>} name="Crypto News" detail="5 TON" />);
    const header = screen.getByTestId('onboarding-channel-header');
    expect(header.className).toContain('am-onboarding-channel-header');
    expect(header).not.toHaveAttribute('style');
  });

  it('renders helper controls via class contract', () => {
    renderWithProviders(<MockupTextButton text="Back to list" onClick={() => {}} />);
    renderWithProviders(<TaskHint text="Tap a channel to continue" />);

    const textButton = screen.getByTestId('onboarding-mockup-text-button');
    const hint = screen.getByTestId('onboarding-task-hint');

    expect(textButton.className).toContain('am-onboarding-mockup-text-button');
    expect(hint.className).toContain('am-onboarding-task-hint');
  });
});
