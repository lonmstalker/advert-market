import { Text } from '@telegram-tools/ui-kit';
import { SearchIcon } from '@/shared/ui';

type MockupSearchBarProps = {
  placeholder: string;
};

export function MockupSearchBar({ placeholder }: MockupSearchBarProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        padding: '10px 12px',
        borderRadius: '10px',
        backgroundColor: 'var(--color-background-secondary)',
        marginBottom: '12px',
      }}
    >
      <SearchIcon width={16} height={16} style={{ color: 'var(--color-text-secondary)', flexShrink: 0 }} />
      <Text type="caption1" color="secondary">
        {placeholder}
      </Text>
    </div>
  );
}
