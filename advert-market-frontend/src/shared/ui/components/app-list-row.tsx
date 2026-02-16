import { Icon, Text } from '@telegram-tools/ui-kit';
import type { ReactNode } from 'react';
import { Tappable } from '@/shared/ui/components/tappable';

type AppListRowProps = {
  label: string;
  description?: string;
  before?: ReactNode;
  value?: ReactNode;
  chevron?: boolean;
  onClick?: () => void;
  className?: string;
  testId?: string;
};

function joinClasses(...classes: Array<string | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}

export function AppListRow({
  label,
  description,
  before,
  value,
  chevron = false,
  onClick,
  className,
  testId,
}: AppListRowProps) {
  const content = (
    <>
      {before ? <div className="am-list-rowBefore">{before}</div> : null}

      <div className="am-list-rowCopy">
        <Text type="body" weight="medium">
          {label}
        </Text>
        {description ? (
          <Text type="caption1" color="secondary">
            {description}
          </Text>
        ) : null}
      </div>

      {value ? <div className="am-list-rowValue">{value}</div> : null}
      {chevron ? (
        <span className="am-list-rowChevron" aria-hidden="true">
          <Icon name="chevron" color="secondary" />
        </span>
      ) : null}
    </>
  );

  if (onClick) {
    return (
      <Tappable
        className={joinClasses('am-surface-row', 'am-list-row', className)}
        onClick={onClick}
        data-testid={testId}
      >
        {content}
      </Tappable>
    );
  }

  return (
    <div className={joinClasses('am-surface-row', 'am-list-row', className)} data-testid={testId}>
      {content}
    </div>
  );
}
