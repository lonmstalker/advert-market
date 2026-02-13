import { Text } from '@telegram-tools/ui-kit';

type TextareaFieldProps = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  maxLength?: number;
  showCharCount?: boolean;
  rows?: number;
  label?: string;
};

export function TextareaField({
  value,
  onChange,
  placeholder,
  maxLength = 2000,
  showCharCount = true,
  rows = 4,
  label,
}: TextareaFieldProps) {
  return (
    <div>
      {(label || showCharCount) && (
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
          {label && (
            <Text type="subheadline2" color="secondary">
              {label}
            </Text>
          )}
          {showCharCount && (
            <Text type="caption1" color="tertiary">
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                {value.length}/{maxLength}
              </span>
            </Text>
          )}
        </div>
      )}
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        maxLength={maxLength}
        rows={rows}
        style={{
          width: '100%',
          padding: '12px 16px',
          borderRadius: 12,
          border: '1px solid var(--color-border-separator)',
          background: 'var(--color-background-base)',
          color: 'var(--color-foreground-primary)',
          fontSize: 16,
          fontFamily: 'inherit',
          lineHeight: 1.4,
          resize: 'vertical',
          outline: 'none',
          boxSizing: 'border-box',
        }}
      />
    </div>
  );
}
