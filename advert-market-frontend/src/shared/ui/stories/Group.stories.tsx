import type { Meta, StoryObj } from '@storybook/react-vite';
import { Group, GroupItem, Icon, Text, Toggle } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useState } from 'react';
import { listItem, staggerChildren } from '../animations';

const meta: Meta<typeof Group> = {
  title: 'UI Kit/Group',
  component: Group,
};

export default meta;
type Story = StoryObj<typeof Group>;

export const Default: Story = {
  render: () => (
    <Group header="Settings">
      <GroupItem text="Profile" chevron />
      <GroupItem text="Notifications" chevron />
      <GroupItem text="Privacy" chevron />
    </Group>
  ),
};

export const WithDescriptions: Story = {
  render: () => (
    <Group header="Account">
      <GroupItem text="Username" description="@advertiser" chevron />
      <GroupItem text="Email" description="user@example.com" chevron />
      <GroupItem text="Phone" description="+7 (999) 123-45-67" chevron />
    </Group>
  ),
};

export const WithToggle: Story = {
  render: function Render() {
    const [notifications, setNotifications] = useState(true);
    const [sound, setSound] = useState(false);
    return (
      <Group header="Notifications">
        <GroupItem text="Push Notifications" after={<Toggle isEnabled={notifications} onChange={setNotifications} />} />
        <GroupItem text="Sound" after={<Toggle isEnabled={sound} onChange={setSound} />} />
      </Group>
    );
  },
};

export const WithIcons: Story = {
  render: () => (
    <Group header="Deal Actions">
      <GroupItem text="Accept" before={<Icon name="check" color="success" size="20px" />} chevron />
      <GroupItem text="Decline" before={<Icon name="cross" color="destructive" size="20px" />} chevron />
      <GroupItem text="Counter-offer" before={<Icon name="chevron" color="accent" size="20px" />} chevron />
    </Group>
  ),
};

export const WithFooter: Story = {
  render: () => (
    <Group header="Wallet" footer="Balance updates may take up to 30 seconds">
      <GroupItem
        text="Balance"
        after={
          <Text type="body" color="accent">
            50.00 TON
          </Text>
        }
      />
      <GroupItem
        text="Pending"
        after={
          <Text type="body" color="secondary">
            10.00 TON
          </Text>
        }
      />
    </Group>
  ),
};

export const Skeleton: Story = {
  render: () => (
    <Group header="Loading..." skeleton={{ show: true }}>
      <GroupItem text="Item 1" />
      <GroupItem text="Item 2" />
      <GroupItem text="Item 3" />
    </Group>
  ),
};

export const AnimatedList: Story = {
  render: () => {
    const items = ['Browse Channels', 'My Deals', 'Wallet', 'Settings'];
    return (
      <motion.div {...staggerChildren} initial="initial" animate="animate">
        <Group header="Navigation">
          {items.map((item) => (
            <motion.div key={item} {...listItem}>
              <GroupItem text={item} chevron />
            </motion.div>
          ))}
        </Group>
      </motion.div>
    );
  },
};

export const MultipleGroups: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <Group header="Active Deals">
        <GroupItem text="Crypto News — Banner" description="50 TON" chevron />
        <GroupItem text="Tech Daily — Post" description="30 TON" chevron />
      </Group>
      <Group header="Completed">
        <GroupItem text="Finance Hub — Story" description="20 TON" />
      </Group>
    </div>
  ),
};
