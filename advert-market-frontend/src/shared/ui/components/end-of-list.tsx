import { Text } from '@telegram-tools/ui-kit';

type EndOfListProps = {
  label: string;
};

export function EndOfList({ label }: EndOfListProps) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '20px 24px 8px' }}>
      <div style={{ flex: 1, height: '0.5px', background: 'var(--color-border-separator)' }} />
      <span style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>
        <Text type="caption1" color="tertiary">
          {label}
        </Text>
      </span>
      <div style={{ flex: 1, height: '0.5px', background: 'var(--color-border-separator)' }} />
    </div>
  );
}
