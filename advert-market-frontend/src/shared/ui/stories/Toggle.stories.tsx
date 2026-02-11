import type { Meta, StoryObj } from '@storybook/react-vite';
import { Group, GroupItem, Toggle } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { tapScale } from '../animations';

const meta: Meta<typeof Toggle> = {
  title: 'UI Kit/Toggle',
  component: Toggle,
  argTypes: {
    disabled: { control: 'boolean' },
  },
};

export default meta;
type Story = StoryObj<typeof Toggle>;

export const Default: Story = {
  render: function Render() {
    const [on, setOn] = useState(false);
    return <Toggle isEnabled={on} onChange={setOn} />;
  },
};

export const Enabled: Story = {
  render: function Render() {
    const [on, setOn] = useState(true);
    return <Toggle isEnabled={on} onChange={setOn} />;
  },
};

export const Disabled: Story = {
  args: { isEnabled: false, disabled: true, onChange: () => {} },
};

export const InGroup: Story = {
  render: function Render() {
    const [push, setPush] = useState(true);
    const [email, setEmail] = useState(false);
    const [deals, setDeals] = useState(true);
    return (
      <Group header="Notifications">
        <GroupItem text="Push" after={<Toggle isEnabled={push} onChange={setPush} />} />
        <GroupItem text="Email" after={<Toggle isEnabled={email} onChange={setEmail} />} />
        <GroupItem text="Deal Updates" after={<Toggle isEnabled={deals} onChange={setDeals} />} />
      </Group>
    );
  },
};

export const AnimatedToggle: Story = {
  render: function Render() {
    const [on, setOn] = useState(false);
    return (
      <motion.div {...tapScale}>
        <Toggle isEnabled={on} onChange={setOn} />
      </motion.div>
    );
  },
};
