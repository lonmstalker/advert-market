import { type CSSProperties, forwardRef, useCallback, useLayoutEffect, useRef } from 'react';

type TextareaProps = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  maxLength?: number;
  rows?: number;
  autosize?: boolean;
  className?: string;
  style?: CSSProperties;
  onFocus?: () => void;
  onBlur?: () => void;
  onSelect?: () => void;
  onKeyUp?: () => void;
  onMouseUp?: () => void;
  onTouchEnd?: () => void;
};

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  {
    value,
    onChange,
    placeholder,
    maxLength,
    rows = 4,
    autosize = false,
    className,
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
  const innerRef = useRef<HTMLTextAreaElement | null>(null);

  const bindRefs = useCallback(
    (node: HTMLTextAreaElement | null) => {
      innerRef.current = node;
      if (typeof ref === 'function') {
        ref(node);
        return;
      }
      if (ref) {
        ref.current = node;
      }
    },
    [ref],
  );

  const handleChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => onChange(e.target.value), [onChange]);

  useLayoutEffect(() => {
    if (!autosize || !innerRef.current) return;

    const element = innerRef.current;
    element.style.height = 'auto';
    const minHeight = rows * 24;
    element.style.height = `${Math.max(minHeight, element.scrollHeight)}px`;
  }, [autosize, rows, value]);

  return (
    <textarea
      ref={bindRefs}
      value={value}
      onChange={handleChange}
      placeholder={placeholder}
      maxLength={maxLength}
      rows={rows}
      className={className ? `am-textarea ${className}` : 'am-textarea'}
      style={style}
      data-autosize={autosize ? 'true' : 'false'}
      onFocus={onFocus}
      onBlur={onBlur}
      onSelect={onSelect}
      onKeyUp={onKeyUp}
      onMouseUp={onMouseUp}
      onTouchEnd={onTouchEnd}
    />
  );
});
