import { describe, expect, it } from 'vitest';
import { render, screen } from '@/test/test-utils';
import { FeatureCard } from './feature-card';

describe('FeatureCard', () => {
  it('renders icon without background box wrapper', () => {
    render(<FeatureCard icon={<span data-testid="custom-icon">*</span>} title="Title" hint="Hint" />);
    expect(screen.getByTestId('custom-icon')).toBeInTheDocument();
    expect(screen.queryByTestId('feature-icon-box')).not.toBeInTheDocument();
  });
});
