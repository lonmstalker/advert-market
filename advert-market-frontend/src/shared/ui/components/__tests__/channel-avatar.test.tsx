import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ChannelAvatar, computeAvatarHue } from '../channel-avatar';

describe('ChannelAvatar', () => {
  it('renders first letter uppercase', () => {
    render(<ChannelAvatar title="test channel" />);
    expect(screen.getByText('T')).toBeInTheDocument();
  });

  it('applies md size by default (44px)', () => {
    const { container } = render(<ChannelAvatar title="Alpha" />);
    const avatar = container.firstChild as HTMLElement;
    expect(avatar.style.width).toBe('44px');
    expect(avatar.style.height).toBe('44px');
  });

  it('applies sm size (40px)', () => {
    const { container } = render(<ChannelAvatar title="Beta" size="sm" />);
    const avatar = container.firstChild as HTMLElement;
    expect(avatar.style.width).toBe('40px');
  });

  it('applies xs size (36px)', () => {
    const { container } = render(<ChannelAvatar title="Delta" size="xs" />);
    const avatar = container.firstChild as HTMLElement;
    expect(avatar.style.width).toBe('36px');
  });

  it('applies lg size (56px)', () => {
    const { container } = render(<ChannelAvatar title="Gamma" size="lg" />);
    const avatar = container.firstChild as HTMLElement;
    expect(avatar.style.width).toBe('56px');
  });
});

describe('computeAvatarHue', () => {
  it('returns consistent hue for same title', () => {
    expect(computeAvatarHue('Test')).toBe(computeAvatarHue('Test'));
  });

  it('returns different hue for different titles', () => {
    expect(computeAvatarHue('Alpha')).not.toBe(computeAvatarHue('Beta'));
  });

  it('returns value 0-359', () => {
    const hue = computeAvatarHue('Z');
    expect(hue).toBeGreaterThanOrEqual(0);
    expect(hue).toBeLessThan(360);
  });
});
