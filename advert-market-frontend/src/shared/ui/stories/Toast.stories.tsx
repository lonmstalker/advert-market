import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button, useToast } from '@telegram-tools/ui-kit';

function ToastDemo({ type, message }: { type: 'success' | 'error' | 'info'; message: string }) {
  const { showToast } = useToast();
  return (
    <Button
      text={`Show ${type}`}
      type={type === 'error' ? 'secondary' : 'primary'}
      onClick={() => showToast(message, { type })}
    />
  );
}

const meta: Meta = {
  title: 'UI Kit/Toast',
  parameters: { layout: 'padded' },
};

export default meta;
type Story = StoryObj;

export const Success: Story = {
  render: () => <ToastDemo type="success" message="Deal created successfully!" />,
};

export const ErrorToast: Story = {
  render: () => <ToastDemo type="error" message="Payment failed. Try again." />,
};

export const Info: Story = {
  render: () => <ToastDemo type="info" message="New deal offer received" />,
};

export const AllTypes: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
      <ToastDemo type="success" message="Payment confirmed — 50 TON" />
      <ToastDemo type="error" message="Insufficient balance" />
      <ToastDemo type="info" message="Creative submitted for review" />
    </div>
  ),
};
