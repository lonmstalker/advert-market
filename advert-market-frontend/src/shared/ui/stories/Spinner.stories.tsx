import type { Meta, StoryObj } from '@storybook/react-vite';
import { Spinner, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { fadeIn } from '../animations';

const meta: Meta<typeof Spinner> = {
  title: 'UI Kit/Spinner',
  component: Spinner,
  argTypes: {
    size: { control: 'text' },
    color: { control: 'select', options: ['primary', 'secondary', 'accent', 'white'] },
  },
};

export default meta;
type Story = StoryObj<typeof Spinner>;

export const Default: Story = {
  args: { size: '24px', color: 'primary' },
};

export const Large: Story = {
  args: { size: '48px', color: 'accent' },
};

export const AllColors: Story = {
  render: () => {
    const colors = ['primary', 'secondary', 'accent'] as const;
    return (
      <div className="flex gap-6 items-center">
        {colors.map((c) => (
          <div key={c} className="flex flex-col items-center gap-2">
            <Spinner size="32px" color={c} />
            <Text type="caption2" color="secondary">
              {c}
            </Text>
          </div>
        ))}
      </div>
    );
  },
};

export const WhiteOnDark: Story = {
  render: () => (
    <div className="flex justify-center p-6 rounded-control" style={{ background: '#1C1C1E' }}>
      <Spinner size="32px" color="white" />
    </div>
  ),
};

export const PageLoader: Story = {
  render: () => (
    <motion.div {...fadeIn} className="flex flex-col items-center gap-3">
      <Spinner size="40px" color="accent" />
      <Text type="callout" color="secondary">
        Loading deals...
      </Text>
    </motion.div>
  ),
};
