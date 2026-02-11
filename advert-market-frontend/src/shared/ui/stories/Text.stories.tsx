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
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
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
      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
        {colors.map((c) => (
          <Text key={c} type="body" color={c}>
            Color: {c}
          </Text>
        ))}
      </div>
    );
  },
};

export const AnimatedHeading: Story = {
  render: () => (
    <AnimatePresence>
      <motion.div {...slideUp}>
        <Text type="title1" weight="bold">
          Animated Title
        </Text>
      </motion.div>
      <motion.div {...fadeIn} style={{ marginTop: '8px' }}>
        <Text type="body" color="secondary">
          Subtitle fades in after heading
        </Text>
      </motion.div>
    </AnimatePresence>
  ),
};
