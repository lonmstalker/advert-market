import type { CSSProperties } from 'react';
import { SearchIcon } from '../icons';

type SearchInputProps = {
  value: string;
  onChange: (value: string) => void;
  onFocus?: () => void;
  onBlur?: () => void;
  placeholder?: string;
  focused?: boolean;
  style?: CSSProperties;
};

export function SearchInput({ value, onChange, onFocus, onBlur, placeholder, focused, style }: SearchInputProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        padding: '10px 12px',
        borderRadius: 12,
        background: 'var(--color-background-base)',
        border: `1.5px solid ${focused ? 'var(--color-accent-primary)' : 'var(--color-border-separator)'}`,
        transition: 'border-color 0.2s ease',
        ...style,
      }}
    >
      <SearchIcon
        style={{
          width: 18,
          height: 18,
          color: focused ? 'var(--color-accent-primary)' : 'var(--color-foreground-tertiary)',
          flexShrink: 0,
          transition: 'color 0.2s ease',
        }}
      />
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={onFocus}
        onBlur={onBlur}
        placeholder={placeholder}
        style={{
          flex: 1,
          border: 'none',
          outline: 'none',
          background: 'transparent',
          fontSize: 15,
          color: 'var(--color-foreground-primary)',
          lineHeight: 1.3,
          fontFamily: 'inherit',
        }}
      />
    </div>
  );
}
