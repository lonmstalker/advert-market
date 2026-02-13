import { renderWithProviders, screen } from '@/test/test-utils';
import { EmptyState } from './empty-state';

describe('EmptyState', () => {
  it('renders emoji, title, and description', () => {
    renderWithProviders(<EmptyState emoji="🔍" title="No results" description="Try again later" />);
    expect(screen.getByText('🔍')).toBeInTheDocument();
    expect(screen.getByText('No results')).toBeInTheDocument();
    expect(screen.getByText('Try again later')).toBeInTheDocument();
  });

  it('does not render CTA button without actionLabel and onAction', () => {
    renderWithProviders(<EmptyState emoji="🔍" title="No results" description="Try again later" />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('renders CTA button when actionLabel and onAction are provided', () => {
    renderWithProviders(
      <EmptyState emoji="🔍" title="No results" description="Try again" actionLabel="Retry" onAction={() => {}} />,
    );
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('calls onAction when CTA button is clicked', async () => {
    const onAction = vi.fn();
    const { user } = renderWithProviders(
      <EmptyState emoji="🔍" title="No results" description="Try again" actionLabel="Retry" onAction={onAction} />,
    );
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onAction).toHaveBeenCalledOnce();
  });

  it('renders icon instead of emoji when icon prop is provided', () => {
    renderWithProviders(
      <EmptyState icon={<svg data-testid="test-icon" />} title="No results" description="Try again" />,
    );
    expect(screen.getByTestId('test-icon')).toBeInTheDocument();
    expect(screen.queryByText('🔍')).not.toBeInTheDocument();
  });
});
