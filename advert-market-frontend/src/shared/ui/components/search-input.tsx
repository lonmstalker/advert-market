import { useHaptic } from '@/shared/hooks/use-haptic';
import { SearchIcon } from '../icons';

type SearchInputProps = {
  value: string;
  onChange: (value: string) => void;
  onFocus?: () => void;
  onBlur?: () => void;
  placeholder?: string;
  focused?: boolean;
  className?: string;
};

export function SearchInput({ value, onChange, onFocus, onBlur, placeholder, focused, className }: SearchInputProps) {
  const haptic = useHaptic();

  return (
    <div className={className ? `am-search-input ${className}` : 'am-search-input'} data-focused={focused}>
      <SearchIcon className="am-search-input__icon" />
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={placeholder}
        onFocus={() => {
          haptic.impactOccurred('light');
          onFocus?.();
        }}
        onBlur={() => onBlur?.()}
        placeholder={placeholder}
        className="am-search-input__field"
      />
    </div>
  );
}
