import type { Meta, StoryObj } from '@storybook/react-vite';

import { Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { fadeIn, slideUp } from '../animations';

const meta: Meta<typeof Text> = {
  title: 'UI Kit/Text',
  component: Text,
  argTypes: {
    type: {
      control: 'select',
      options: [
        'hero',
        'largeTitle',
        'title1',
        'title2',
        'title3',
        'title4',
        'body',
        'callout',
        'subheadline1',
        'subheadline2',
        'footnote',
        'caption1',
        'caption2',
      ],
    },
    weight: { control: 'select', options: ['light', 'regular', 'medium', 'bold'] },
    color: { control: 'select', options: ['primary', 'secondary', 'tertiary', 'accent', 'danger', 'white'] },
    align: { control: 'select', options: ['left', 'center', 'right', 'stretch'] },
    uppercase: { control: 'boolean' },
  },
};

export default meta;
type Story = StoryObj<typeof Text>;

export const Default: Story = {
  args: { children: 'Hello, Ad Market!', type: 'body' },
};

export const Hero: Story = {
  args: { children: 'Ad Market', type: 'hero', weight: 'bold' },
};

export const AllSizes: Story = {
  render: () => {
    const types = [
      'hero',
      'largeTitle',
      'title1',
      'title2',
      'title3',
      'title4',
      'body',
      'callout',
      'subheadline1',
      'subheadline2',
      'footnote',
      'caption1',
      'caption2',
    ] as const;
    return (
      <div className="flex flex-col gap-2">
        {types.map((t) => (
          <Text key={t} type={t}>
            {t}
          </Text>
        ))}
      </div>
    );
  },
};

export const Colors: Story = {
  render: () => {
    const colors = ['primary', 'secondary', 'tertiary', 'accent', 'danger'] as const;
    return (
      <div className="flex flex-col gap-1">
        {colors.map((c) => (
          <Text key={c} type="body" color={c}>
            Color: {c}
          </Text>
        ))}
      </div>
    );
  },
};

export const FinancialTypography: Story = {
  name: 'Financial Typography',
  render: () => (
    <div className="flex flex-col gap-4">
      <div>
        <Text type="caption1" color="secondary">
          hero bold — Main balance
        </Text>
        <Text type="hero" weight="bold">
          <span className="tabular-nums">1 250.50 TON</span>
        </Text>
      </div>
      <div>
        <Text type="caption1" color="secondary">
          largeTitle bold — Compact balance
        </Text>
        <Text type="largeTitle" weight="bold">
          <span className="tabular-nums">250.00 TON</span>
        </Text>
      </div>
      <div>
        <Text type="caption1" color="secondary">
          callout accent — Amount in list
        </Text>
        <Text type="callout" color="accent">
          <span className="tabular-nums">50.00 TON</span>
        </Text>
      </div>
      <div>
        <Text type="caption1" color="secondary">
          callout success — Income
        </Text>
        <Text type="callout" color="accent">
          <span className="tabular-nums" style={{ color: 'var(--color-state-success)' }}>
            +30.00 TON
          </span>
        </Text>
      </div>
      <div>
        <Text type="caption1" color="secondary">
          callout destructive — Expense
        </Text>
        <Text type="callout" color="danger">
          <span className="tabular-nums">-50.00 TON</span>
        </Text>
      </div>
    </div>
  ),
};

export const AnimatedHeading: Story = {
  render: () => (
    <AnimatePresence>
      <motion.div {...slideUp}>
        <Text type="title1" weight="bold">
          Animated Title
        </Text>
      </motion.div>
      <motion.div {...fadeIn} className="mt-2">
        <Text type="body" color="secondary">
          Subtitle fades in after heading
        </Text>
      </motion.div>
    </AnimatePresence>
  ),
};
