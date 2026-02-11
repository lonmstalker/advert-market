import { Button, Group, GroupItem, Text } from '@telegram-tools/ui-kit';

export default function HomePage() {
  return (
    <div style={{ padding: '16px' }}>
      <Text type="title1" weight="bold">
        Ad Market
      </Text>
      <Text type="body" color="secondary" style={{ marginTop: '8px' }}>
        Marketplace for Telegram channel advertising
      </Text>

      <Group header="Quick Actions" style={{ marginTop: '24px' }}>
        <GroupItem text="Browse Channels" chevron />
        <GroupItem text="My Deals" chevron />
        <GroupItem text="Wallet" chevron />
      </Group>

      <div style={{ marginTop: '24px' }}>
        <Button text="Create Ad Request" type="primary" />
      </div>
    </div>
  );
}
