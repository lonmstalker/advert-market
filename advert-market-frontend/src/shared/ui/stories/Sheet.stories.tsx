import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button, Group, GroupItem, Sheet, Text } from '@telegram-tools/ui-kit';
import { useState } from 'react';

function DealDetails() {
  return (
    <div style={{ padding: '16px' }}>
      <Text type="title2" weight="bold">
        Deal Details
      </Text>
      <Group header="Info" style={{ marginTop: '16px' }}>
        <GroupItem text="Channel" description="Crypto News" />
        <GroupItem text="Amount" description="50 TON" />
        <GroupItem text="Status" description="Pending" />
      </Group>
    </div>
  );
}

function FilterPanel() {
  return (
    <div style={{ padding: '16px' }}>
      <Text type="title2" weight="bold">
        Filters
      </Text>
      <Group header="Category" style={{ marginTop: '16px' }}>
        <GroupItem text="Crypto" chevron />
        <GroupItem text="Tech" chevron />
        <GroupItem text="Finance" chevron />
      </Group>
    </div>
  );
}

const sheets = {
  deal: DealDetails,
  filter: FilterPanel,
};

const meta: Meta<typeof Sheet> = {
  title: 'UI Kit/Sheet',
  component: Sheet,
  parameters: { layout: 'padded' },
};

export default meta;
type Story = StoryObj<typeof Sheet>;

export const DealSheet: Story = {
  render: function Render() {
    const [opened, setOpened] = useState(false);
    return (
      <>
        <Button text="Show Deal" type="primary" onClick={() => setOpened(true)} />
        <Sheet sheets={sheets} activeSheet="deal" opened={opened} onClose={() => setOpened(false)} />
      </>
    );
  },
};

export const FilterSheet: Story = {
  render: function Render() {
    const [opened, setOpened] = useState(false);
    return (
      <>
        <Button text="Open Filters" type="secondary" onClick={() => setOpened(true)} />
        <Sheet sheets={sheets} activeSheet="filter" opened={opened} onClose={() => setOpened(false)} />
      </>
    );
  },
};
