import { type CSSProperties, forwardRef, useCallback } from 'react';

type TextareaProps = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  maxLength?: number;
  rows?: number;
  style?: CSSProperties;
  onFocus?: () => void;
  onBlur?: () => void;
  onSelect?: () => void;
  onKeyUp?: () => void;
  onMouseUp?: () => void;
  onTouchEnd?: () => void;
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

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  {
    value,
    onChange,
    placeholder,
    maxLength,
    rows = 4,
    style,
    onFocus,
    onBlur,
    onSelect,
    onKeyUp,
    onMouseUp,
    onTouchEnd,
  },
  ref,
) {
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
      onFocus={onFocus}
      onBlur={onBlur}
      onSelect={onSelect}
      onKeyUp={onKeyUp}
      onMouseUp={onMouseUp}
      onTouchEnd={onTouchEnd}
    />
  );
});
