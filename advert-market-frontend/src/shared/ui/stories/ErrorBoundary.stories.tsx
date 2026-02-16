import type { Meta, StoryObj } from '@storybook/react-vite';
import { Text } from '@telegram-tools/ui-kit';
import { ErrorBoundary } from '../components/error-boundary';

function CrashComponent(): never {
  throw new Error('Storybook crash scenario');
}

const meta: Meta<typeof ErrorBoundary> = {
  title: 'Ad Market/Infrastructure/ErrorBoundary',
  component: ErrorBoundary,
};

export default meta;
type Story = StoryObj<typeof meta>;

export const HealthyState: Story = {
  render: () => (
    <ErrorBoundary>
      <div style={{ minHeight: '50vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Text type="title3" weight="medium">
          Child content rendered successfully
        </Text>
      </div>
    </ErrorBoundary>
  ),
};

export const FallbackState: Story = {
  render: () => (
    <ErrorBoundary>
      <CrashComponent />
    </ErrorBoundary>
  ),
};
