import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LanguageBadge } from '../language-badge';

describe('LanguageBadge', () => {
  it('renders language code in uppercase', () => {
    render(<LanguageBadge code="en" />);
    expect(screen.getByText('en')).toBeInTheDocument();
    expect(screen.getByText('en').style.textTransform).toBe('uppercase');
  });

  it('applies md size by default', () => {
    const { container } = render(<LanguageBadge code="ru" />);
    const badge = container.firstChild as HTMLElement;
    expect(badge.style.fontSize).toBe('11px');
  });

  it('applies sm size', () => {
    const { container } = render(<LanguageBadge code="fr" size="sm" />);
    const badge = container.firstChild as HTMLElement;
    expect(badge.style.fontSize).toBe('10px');
  });
});
