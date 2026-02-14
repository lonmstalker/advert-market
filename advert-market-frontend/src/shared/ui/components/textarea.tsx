import { type CSSProperties, type Ref, useCallback } from 'react';

type TextareaProps = {
  ref?: Ref<HTMLTextAreaElement>;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  maxLength?: number;
  rows?: number;
  style?: CSSProperties;
};

const baseStyle: CSSProperties = {
  width: '100%',
  resize: 'vertical',
  padding: 12,
  borderRadius: 12,
  border: '1px solid var(--color-border-separator)',
  background: 'var(--color-background-base)',
  color: 'var(--color-foreground-primary)',
  fontSize: 15,
  lineHeight: 1.4,
  fontFamily: 'inherit',
  boxSizing: 'border-box',
  outline: 'none',
};

export function Textarea({ ref, value, onChange, placeholder, maxLength, rows = 4, style }: TextareaProps) {
  const handleChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => onChange(e.target.value), [onChange]);

  return (
    <textarea
      ref={ref}
      value={value}
      onChange={handleChange}
      placeholder={placeholder}
      maxLength={maxLength}
      rows={rows}
      style={{ ...baseStyle, ...style }}
    />
  );
}
