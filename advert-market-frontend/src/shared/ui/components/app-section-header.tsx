import { Text } from '@telegram-tools/ui-kit';
import type { ReactNode } from 'react';

type AppSectionHeaderProps = {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  className?: string;
  testId?: string;
};

function joinClasses(...classes: Array<string | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}

export function AppSectionHeader({ title, subtitle, action, className, testId }: AppSectionHeaderProps) {
  return (
    <header className={joinClasses('am-section-header', className)} data-testid={testId}>
      <div className="am-section-headerCopy">
        <Text type="title2" weight="bold">
          {title}
        </Text>
        {subtitle ? (
          <div className="am-section-headerSubtitle">
            <Text type="subheadline2" color="secondary">
              {subtitle}
            </Text>
          </div>
        ) : null}
      </div>
      {action ? <div className="am-section-headerAction">{action}</div> : null}
    </header>
  );
}
