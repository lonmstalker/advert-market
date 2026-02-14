import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { pressScale } from '../animations';
import { TonDiamondIcon } from '../icons';

const meta: Meta<typeof Button> = {
  title: 'UI Kit/Button',
  component: Button,
  argTypes: {
    type: { control: 'radio', options: ['primary', 'secondary'] },
    disabled: { control: 'boolean' },
    loading: { control: 'boolean' },
    text: { control: 'text' },
  },
};

export default meta;
type Story = StoryObj<typeof Button>;

export const Primary: Story = {
  args: { text: 'Create Deal', type: 'primary' },
};

export const Secondary: Story = {
  args: { text: 'Cancel', type: 'secondary' },
};

export const Loading: Story = {
  args: { text: 'Processing...', type: 'primary', loading: true },
};

export const Disabled: Story = {
  args: { text: 'Unavailable', type: 'primary', disabled: true },
};

export const WithIcon: Story = {
  render: (args) => <Button {...args} icon={<TonDiamondIcon style={{ width: 18, height: 18 }} />} />,
  args: { text: 'Send TON', type: 'primary' },
};

export const Animated: Story = {
  render: (args) => (
    <motion.div {...pressScale}>
      <Button {...args} />
    </motion.div>
  ),
  args: { text: 'Tap me', type: 'primary' },
};

export const PairActions: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: '12px', width: '100%' }}>
      <motion.div {...pressScale} style={{ flex: 1 }}>
        <Button text="Decline" type="secondary" />
      </motion.div>
      <motion.div {...pressScale} style={{ flex: 1 }}>
        <Button text="Accept" type="primary" />
      </motion.div>
    </div>
  ),
};
