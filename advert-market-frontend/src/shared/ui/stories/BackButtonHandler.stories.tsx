import type { Meta, StoryObj } from '@storybook/react-vite';
import { Text } from '@telegram-tools/ui-kit';
import { BackButtonHandler } from '../components/back-button-handler';

const meta: Meta<typeof BackButtonHandler> = {
  title: 'Ad Market/Infrastructure/BackButtonHandler',
  component: BackButtonHandler,
  parameters: {
    route: '/deals/42',
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const NestedRoute: Story = {
  parameters: {
    route: '/deals/42',
  },
  render: () => (
    <div style={{ display: 'grid', gap: 8 }}>
      <BackButtonHandler />
      <Text type="subheadline2">
        Компонент не имеет визуального вывода. На вложенных маршрутах включает Telegram BackButton.
      </Text>
    </div>
  ),
};

export const RootRoute: Story = {
  parameters: {
    route: '/catalog',
  },
  render: () => (
    <div style={{ display: 'grid', gap: 8 }}>
      <BackButtonHandler />
      <Text type="subheadline2">На корневых маршрутах (`/catalog`, `/onboarding`) Telegram BackButton скрывается.</Text>
    </div>
  ),
};
