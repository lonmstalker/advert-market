import { Text } from '@telegram-tools/ui-kit';
import type { ReactNode } from 'react';

type ChannelHeaderProps = {
  icon: ReactNode;
  name: string;
  detail: string;
};

export function ChannelHeader({ icon, name, detail }: ChannelHeaderProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        padding: '4px 0 8px',
      }}
    >
      <div
        aria-hidden="true"
        style={{
          width: '36px',
          height: '36px',
          borderRadius: '50%',
          backgroundColor: 'var(--color-background-secondary)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}
      >
        {icon}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <Text type="subheadline1" weight="medium">
          {name}
        </Text>
        <Text type="caption1" color="accent">
          {detail}
        </Text>
      </div>
    </div>
  );
}
