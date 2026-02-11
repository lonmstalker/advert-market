import type { Meta, StoryObj } from '@storybook/react-vite';
import { Select } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { fadeIn } from '../animations';

const meta: Meta<typeof Select> = {
  title: 'UI Kit/Select',
  component: Select,
};

export default meta;
type Story = StoryObj<typeof Select>;

const topicOptions = [
  { label: 'Crypto', value: 'crypto' },
  { label: 'Finance', value: 'finance' },
  { label: 'Tech', value: 'tech' },
  { label: 'Lifestyle', value: 'lifestyle' },
];

const postTypeOptions = [
  { label: 'Standard Post', value: 'STANDARD' },
  { label: 'Pinned Post', value: 'PINNED' },
  { label: 'Story', value: 'STORY' },
  { label: 'Repost', value: 'REPOST' },
  { label: 'Native', value: 'NATIVE' },
];

export const Default: Story = {
  render: function Render() {
    const [value, setValue] = useState<string | null>(null);
    return <Select options={topicOptions} value={value} onChange={setValue} />;
  },
};

export const WithPreselected: Story = {
  render: function Render() {
    const [value, setValue] = useState<string | null>('crypto');
    return <Select options={topicOptions} value={value} onChange={setValue} />;
  },
};

export const PostTypes: Story = {
  render: function Render() {
    const [value, setValue] = useState<string | null>('STANDARD');
    return <Select options={postTypeOptions} value={value} onChange={setValue} />;
  },
};

export const AnimatedSelect: Story = {
  render: function Render() {
    const [value, setValue] = useState<string | null>(null);
    return (
      <motion.div {...fadeIn}>
        <Select options={topicOptions} value={value} onChange={setValue} />
      </motion.div>
    );
  },
};
