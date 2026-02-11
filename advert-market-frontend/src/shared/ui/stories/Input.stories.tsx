import type { Meta, StoryObj } from '@storybook/react-vite';
import { Input } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { scaleIn } from '../animations';

const meta: Meta<typeof Input> = {
  title: 'UI Kit/Input',
  component: Input,
  argTypes: {
    type: { control: 'select', options: ['text', 'email', 'password', 'number', 'tel', 'url', 'search'] },
    disabled: { control: 'boolean' },
    readOnly: { control: 'boolean' },
    placeholder: { control: 'text' },
  },
};

export default meta;
type Story = StoryObj<typeof Input>;

export const Default: Story = {
  render: function Render() {
    const [value, setValue] = useState('');
    return <Input value={value} onChange={setValue} placeholder="Enter channel name..." />;
  },
};

export const WithError: Story = {
  render: function Render() {
    const [value, setValue] = useState('abc');
    return <Input value={value} onChange={setValue} error="Amount must be a number" />;
  },
};

export const WithSuccess: Story = {
  render: function Render() {
    const [value, setValue] = useState('50');
    return <Input value={value} onChange={setValue} success placeholder="Amount in TON" />;
  },
};

export const Numeric: Story = {
  render: function Render() {
    const [value, setValue] = useState('');
    return <Input value={value} onChange={setValue} numeric placeholder="0.00 TON" />;
  },
};

export const WithValidator: Story = {
  render: function Render() {
    const [value, setValue] = useState('');
    return (
      <Input
        value={value}
        onChange={setValue}
        placeholder="Enter price (min 1 TON)"
        numeric
        validateOnBlur
        validator={(v) => {
          const num = Number(v);
          if (Number.isNaN(num) || num < 1) return 'Minimum 1 TON';
          return null;
        }}
      />
    );
  },
};

export const Disabled: Story = {
  render: () => <Input value="Cannot edit" disabled onChange={() => {}} />,
};

export const Password: Story = {
  render: function Render() {
    const [value, setValue] = useState('');
    return <Input value={value} onChange={setValue} type="password" placeholder="Enter password" />;
  },
};

export const AnimatedAppear: Story = {
  render: function Render() {
    const [value, setValue] = useState('');
    return (
      <motion.div {...scaleIn}>
        <Input value={value} onChange={setValue} placeholder="Animated input" />
      </motion.div>
    );
  },
};
