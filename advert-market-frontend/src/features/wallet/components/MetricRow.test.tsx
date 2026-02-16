import { renderWithProviders, screen } from '@/test/test-utils';
import { MetricRow } from './MetricRow';

function renderRow(props: Partial<Parameters<typeof MetricRow>[0]> = {}) {
  return renderWithProviders(<MetricRow escrowAmount="5000000000" completedDealsCount={12} {...props} />);
}

describe('MetricRow', () => {
  it('renders formatted escrow amount', () => {
    renderRow();
    expect(screen.getByText('5 TON')).toBeInTheDocument();
  });

  it('renders completed deals count', () => {
    renderRow();
    expect(screen.getByText('12')).toBeInTheDocument();
  });

  it('renders escrow label', () => {
    renderRow();
    expect(screen.getByText('In escrow')).toBeInTheDocument();
  });

  it('renders completed deals label', () => {
    renderRow();
    expect(screen.getByText('Completed deals')).toBeInTheDocument();
  });

  it('renders container with divider using CSS class', () => {
    const { container } = renderRow();
    const divider = container.querySelector('[data-testid="metric-divider"]');
    expect(divider).toBeInTheDocument();
    expect(divider).toHaveClass('am-metric-row__divider');
  });

  it('renders escrow value with am-tabnum class', () => {
    renderRow();
    const escrowVal = screen.getByText('5 TON');
    expect(escrowVal).toHaveClass('am-tabnum');
  });
});
