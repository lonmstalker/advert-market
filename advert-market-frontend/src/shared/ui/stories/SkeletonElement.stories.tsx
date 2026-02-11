import type { Meta, StoryObj } from '@storybook/react-vite';
import { Group, GroupItem, SkeletonElement } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { pulse } from '../animations';

const meta: Meta<typeof SkeletonElement> = {
  title: 'UI Kit/SkeletonElement',
  component: SkeletonElement,
};

export default meta;
type Story = StoryObj<typeof SkeletonElement>;

export const Default: Story = {
  render: () => <SkeletonElement style={{ width: '200px', height: '20px', borderRadius: '6px' }} />,
};

export const Circle: Story = {
  render: () => <SkeletonElement style={{ width: '48px', height: '48px', borderRadius: '50%' }} />,
};

export const Card: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', width: '320px' }}>
      <SkeletonElement style={{ width: '100%', height: '160px', borderRadius: '12px' }} />
      <SkeletonElement style={{ width: '70%', height: '16px', borderRadius: '6px' }} />
      <SkeletonElement style={{ width: '40%', height: '14px', borderRadius: '6px' }} />
    </div>
  ),
};

export const ListItem: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: '12px', alignItems: 'center', width: '320px' }}>
      <SkeletonElement style={{ width: '48px', height: '48px', borderRadius: '50%', flexShrink: 0 }} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', flex: 1 }}>
        <SkeletonElement style={{ width: '60%', height: '14px', borderRadius: '6px' }} />
        <SkeletonElement style={{ width: '80%', height: '12px', borderRadius: '6px' }} />
      </div>
    </div>
  ),
};

export const GroupSkeleton: Story = {
  render: () => (
    <Group header="Channels" skeleton={{ show: true }}>
      <GroupItem text="Channel 1" />
      <GroupItem text="Channel 2" />
      <GroupItem text="Channel 3" />
    </Group>
  ),
};

export const PulsingAnimation: Story = {
  render: () => (
    <motion.div {...pulse}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', width: '320px' }}>
        <SkeletonElement style={{ width: '100%', height: '48px', borderRadius: '12px' }} />
        <SkeletonElement style={{ width: '75%', height: '16px', borderRadius: '6px' }} />
        <SkeletonElement style={{ width: '50%', height: '14px', borderRadius: '6px' }} />
      </div>
    </motion.div>
  ),
};
