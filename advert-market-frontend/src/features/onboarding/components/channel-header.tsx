import { Text } from '@telegram-tools/ui-kit';
import type { ReactNode } from 'react';

type ChannelHeaderProps = {
  icon: ReactNode;
  name: string;
  detail: string;
};

export function ChannelHeader({ icon, name, detail }: ChannelHeaderProps) {
  return (
    <div data-testid="onboarding-channel-header" className="am-onboarding-channel-header">
      <div aria-hidden="true" className="am-onboarding-channel-header__icon">
        {icon}
      </div>
      <div className="am-onboarding-channel-header__copy">
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
