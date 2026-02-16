import type { Meta, StoryObj } from '@storybook/react-vite';
import { Text } from '@telegram-tools/ui-kit';
import { useState } from 'react';
import { DeviceFrame } from '../components/device-frame/DeviceFrame';
import { EmptyState } from '../components/empty-state';
import { PageLoader } from '../components/page-loader';
import { PulsingDot } from '../components/pulsing-dot';
import { SearchOffIcon } from '../icons';

const meta: Meta = {
  title: 'Ad Market/Feedback',
};

export default meta;
type Story = StoryObj;

export const EmptyStateAndStatus: Story = {
  render: function Render() {
    const [retryCount, setRetryCount] = useState(0);

    return (
      <div style={{ display: 'grid', gap: 20 }}>
        <EmptyState
          icon={<SearchOffIcon size={34} color="var(--color-foreground-tertiary)" />}
          title="Каналы не найдены"
          description="Измените фильтры и повторите поиск"
          actionLabel="Сбросить фильтры"
          onAction={() => setRetryCount((count) => count + 1)}
        />

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <PulsingDot color="accent" />
          <Text type="caption1" color="secondary">
            Состояние обновлено {retryCount} раз
          </Text>
        </div>
      </div>
    );
  },
};

export const Loader: Story = {
  render: () => <PageLoader />,
};

export const DeviceFramePreview: Story = {
  render: () => (
    <DeviceFrame>
      <div style={{ padding: 16, display: 'grid', gap: 12 }}>
        <Text type="title3" weight="bold">
          Telegram-style frame
        </Text>
        <Text type="caption1" color="secondary">
          Используйте для mobile snapshot stories.
        </Text>
      </div>
    </DeviceFrame>
  ),
};
