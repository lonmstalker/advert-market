import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@telegram-tools/ui-kit';
import { BottomTabs } from '../components/bottom-tabs';
import { FixedBottomBar } from '../components/fixed-bottom-bar';

const meta: Meta = {
  title: 'Ad Market/Navigation',
  parameters: {
    route: '/catalog',
  },
};

export default meta;
type Story = StoryObj;

export const BottomTabsCatalogActive: Story = {
  parameters: {
    route: '/catalog',
  },
  render: () => <BottomTabs />,
};

export const BottomTabsFinanceActive: Story = {
  parameters: {
    route: '/wallet',
  },
  render: () => <BottomTabs />,
};

export const FixedBottomBarActions: Story = {
  render: () => (
    <div style={{ minHeight: '60vh' }}>
      <FixedBottomBar>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <Button text="Отмена" type="secondary" />
          <Button text="Продолжить" type="primary" />
        </div>
      </FixedBottomBar>
      <div className="am-fixed-bottom-bar-spacer" />
    </div>
  ),
};
