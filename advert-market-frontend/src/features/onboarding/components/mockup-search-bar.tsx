import { Text } from '@telegram-tools/ui-kit';
import { SearchIcon } from '@/shared/ui';

type MockupSearchBarProps = {
  placeholder: string;
};

export function MockupSearchBar({ placeholder }: MockupSearchBarProps) {
  return (
    <div className="am-onboarding-search-bar">
      <SearchIcon width={16} height={16} className="am-onboarding-search-bar__icon" />
      <Text type="caption1" color="secondary">
        {placeholder}
      </Text>
    </div>
  );
}
