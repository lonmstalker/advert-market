import { Text } from '@telegram-tools/ui-kit';

type EndOfListProps = {
  label: string;
};

export function EndOfList({ label }: EndOfListProps) {
  return (
    <div className="flex items-center gap-3 pt-5 px-6 pb-2">
      <div className="flex-1 h-px bg-separator" />
      <Text type="caption1" color="tertiary">
        {label}
      </Text>
      <div className="flex-1 h-px bg-separator" />
    </div>
  );
}
