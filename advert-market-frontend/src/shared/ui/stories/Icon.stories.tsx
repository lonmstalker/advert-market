import type { Meta, StoryObj } from '@storybook/react-vite';
import { Icon, Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { scaleIn } from '../animations';

const meta: Meta<typeof Icon> = {
  title: 'UI Kit/Icon',
  component: Icon,
  argTypes: {
    name: { control: 'select', options: ['cross', 'chevron', 'doubleChevron', 'check'] },
    color: {
      control: 'select',
      options: ['default', 'primary', 'secondary', 'tertiary', 'accent', 'destructive', 'warning', 'success'],
    },
    size: { control: 'text' },
  },
};

export default meta;
type Story = StoryObj<typeof Icon>;

export const Cross: Story = {
  args: { name: 'cross', size: '24px', color: 'default' },
};

export const Chevron: Story = {
  args: { name: 'chevron', size: '24px', color: 'default' },
};

export const Check: Story = {
  args: { name: 'check', size: '24px', color: 'success' },
};

export const DoubleChevron: Story = {
  args: { name: 'doubleChevron', size: '24px', color: 'accent' },
};

export const AllIcons: Story = {
  render: () => {
    const names = ['cross', 'chevron', 'doubleChevron', 'check'] as const;
    return (
      <div style={{ display: 'flex', gap: '24px', alignItems: 'center' }}>
        {names.map((name) => (
          <div key={name} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
            <Icon name={name} size="28px" />
            <Text type="caption2" color="secondary">
              {name}
            </Text>
          </div>
        ))}
      </div>
    );
  },
};

export const AllColors: Story = {
  render: () => {
    const colors = [
      'default',
      'primary',
      'secondary',
      'tertiary',
      'accent',
      'destructive',
      'warning',
      'success',
    ] as const;
    return (
      <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
        {colors.map((color) => (
          <div key={color} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
            <Icon name="check" size="24px" color={color} />
            <Text type="caption2" color="secondary">
              {color}
            </Text>
          </div>
        ))}
      </div>
    );
  },
};

export const AnimatedIcon: Story = {
  render: () => (
    <motion.div {...scaleIn}>
      <Icon name="check" size="48px" color="success" />
    </motion.div>
  ),
};

export const RotatingChevron: Story = {
  render: () => (
    <motion.div
      animate={{ rotate: [0, 90, 180, 270, 360] }}
      transition={{ duration: 2, repeat: Number.POSITIVE_INFINITY, ease: 'linear' }}
    >
      <Icon name="chevron" size="32px" color="accent" />
    </motion.div>
  ),
};
