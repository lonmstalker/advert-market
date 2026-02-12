import { renderWithProviders, screen } from '@/test/test-utils';
import { MiniTimeline } from './mini-timeline';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

const baseSteps = [
  { label: 'Find Channel', status: 'completed' as const },
  { label: 'Create Deal', status: 'active' as const, description: 'Negotiate terms with channel owner' },
  { label: 'Escrow Payment', status: 'pending' as const },
];

describe('MiniTimeline', () => {
  it('renders all step labels', () => {
    renderWithProviders(<MiniTimeline steps={baseSteps} />);
    expect(screen.getByText('Find Channel')).toBeInTheDocument();
    expect(screen.getByText('Create Deal')).toBeInTheDocument();
    expect(screen.getByText('Escrow Payment')).toBeInTheDocument();
  });

  it('shows connector line for all steps except last', () => {
    renderWithProviders(<MiniTimeline steps={baseSteps} />);
    const list = screen.getByRole('list', { name: 'Deal timeline' });
    const items = list.querySelectorAll('li');
    expect(items).toHaveLength(3);
  });

  it('invokes onActiveClick when active step is clicked', async () => {
    const onActiveClick = vi.fn();
    const { user } = renderWithProviders(<MiniTimeline steps={baseSteps} onActiveClick={onActiveClick} />);
    await user.click(screen.getByRole('button', { name: /Create Deal/ }));
    expect(onActiveClick).toHaveBeenCalledTimes(1);
  });

  it('does not invoke onActiveClick for completed steps', async () => {
    const onActiveClick = vi.fn();
    const { user } = renderWithProviders(<MiniTimeline steps={baseSteps} onActiveClick={onActiveClick} />);
    await user.click(screen.getByRole('button', { name: /Find Channel/ }));
    expect(onActiveClick).not.toHaveBeenCalled();
  });

  it('expands step description when expandedIndex matches', () => {
    renderWithProviders(<MiniTimeline steps={baseSteps} expandedIndex={1} />);
    expect(screen.getByText('Negotiate terms with channel owner')).toBeInTheDocument();
  });

  it('collapses description when expandedIndex is null', () => {
    renderWithProviders(<MiniTimeline steps={baseSteps} expandedIndex={null} />);
    expect(screen.queryByText('Negotiate terms with channel owner')).not.toBeInTheDocument();
  });

  it('invokes onStepClick when step with description is clicked', async () => {
    const onStepClick = vi.fn();
    const { user } = renderWithProviders(<MiniTimeline steps={baseSteps} onStepClick={onStepClick} />);
    await user.click(screen.getByRole('button', { name: /Create Deal/ }));
    expect(onStepClick).toHaveBeenCalledWith(1);
  });

  it('sets aria-expanded on buttons', () => {
    renderWithProviders(<MiniTimeline steps={baseSteps} expandedIndex={1} />);
    const buttons = screen.getAllByRole('button');
    expect(buttons[0]).toHaveAttribute('aria-expanded', 'false');
    expect(buttons[1]).toHaveAttribute('aria-expanded', 'true');
    expect(buttons[2]).toHaveAttribute('aria-expanded', 'false');
  });
});
