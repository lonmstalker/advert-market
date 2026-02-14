import type { CSSProperties } from 'react';
import { Tappable } from '@/shared/ui';
import { TextEntityType } from '../types/creative';

type FormattingToolbarProps = {
  onFormat: (type: TextEntityType) => void;
  onLink: () => void;
  activeTypes: Set<TextEntityType>;
  disabled?: boolean;
};

const toolbarStyle: CSSProperties = {
  display: 'flex',
  gap: 4,
  padding: '4px 0',
};

const buttonBase: CSSProperties = {
  width: 36,
  height: 36,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  borderRadius: 8,
  border: 'none',
  cursor: 'pointer',
  fontSize: 15,
  fontWeight: 600,
  transition: 'background 0.15s',
};

const BUTTONS: Array<{ type: TextEntityType; label: string; style?: CSSProperties }> = [
  { type: TextEntityType.BOLD, label: 'B', style: { fontWeight: 800 } },
  { type: TextEntityType.ITALIC, label: 'I', style: { fontStyle: 'italic' } },
  { type: TextEntityType.UNDERLINE, label: 'U', style: { textDecoration: 'underline' } },
  { type: TextEntityType.STRIKETHROUGH, label: 'S', style: { textDecoration: 'line-through' } },
  { type: TextEntityType.CODE, label: '</>' },
  { type: TextEntityType.SPOILER, label: '\u2588\u2588' },
];

export function FormattingToolbar({ onFormat, onLink, activeTypes, disabled }: FormattingToolbarProps) {
  return (
    <div style={toolbarStyle}>
      {BUTTONS.map((btn) => {
        const isActive = activeTypes.has(btn.type);
        return (
          <Tappable
            key={btn.type}
            disabled={disabled}
            onClick={() => onFormat(btn.type)}
            style={{
              ...buttonBase,
              ...btn.style,
              background: isActive ? 'var(--color-accent-primary)' : 'var(--color-background-section)',
              color: isActive ? 'var(--color-static-white)' : 'var(--color-foreground-primary)',
              opacity: disabled ? 0.4 : 1,
            }}
            aria-pressed={isActive}
            aria-label={btn.type}
          >
            {btn.label}
          </Tappable>
        );
      })}
      <Tappable
        disabled={disabled}
        onClick={onLink}
        style={{
          ...buttonBase,
          background: activeTypes.has(TextEntityType.TEXT_LINK)
            ? 'var(--color-accent-primary)'
            : 'var(--color-background-section)',
          color: activeTypes.has(TextEntityType.TEXT_LINK)
            ? 'var(--color-static-white)'
            : 'var(--color-foreground-primary)',
          opacity: disabled ? 0.4 : 1,
        }}
        aria-label="Link"
      >
        {'\uD83D\uDD17'}
      </Tappable>
    </div>
  );
}
