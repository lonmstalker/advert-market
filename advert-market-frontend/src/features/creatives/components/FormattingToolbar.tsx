import type { ComponentType, CSSProperties, SVGProps } from 'react';
import { useTranslation } from 'react-i18next';
import { Tappable } from '@/shared/ui';
import {
  FormatBoldIcon,
  FormatCodeIcon,
  FormatItalicIcon,
  FormatSpoilerIcon,
  FormatStrikethroughIcon,
  FormatUnderlineIcon,
  LinkIcon,
} from '@/shared/ui/icons';
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
  transition: 'background 0.15s',
};

const iconStyle: CSSProperties = {
  width: 18,
  height: 18,
};

const BUTTONS: Array<{ type: TextEntityType; icon: ComponentType<SVGProps<SVGSVGElement>>; labelKey: string }> = [
  { type: TextEntityType.BOLD, icon: FormatBoldIcon, labelKey: 'creatives.form.formatting.bold' },
  { type: TextEntityType.ITALIC, icon: FormatItalicIcon, labelKey: 'creatives.form.formatting.italic' },
  { type: TextEntityType.UNDERLINE, icon: FormatUnderlineIcon, labelKey: 'creatives.form.formatting.underline' },
  {
    type: TextEntityType.STRIKETHROUGH,
    icon: FormatStrikethroughIcon,
    labelKey: 'creatives.form.formatting.strikethrough',
  },
  { type: TextEntityType.CODE, icon: FormatCodeIcon, labelKey: 'creatives.form.formatting.code' },
  { type: TextEntityType.SPOILER, icon: FormatSpoilerIcon, labelKey: 'creatives.form.formatting.spoiler' },
];

export function FormattingToolbar({ onFormat, onLink, activeTypes, disabled }: FormattingToolbarProps) {
  const { t } = useTranslation();

  return (
    <div style={toolbarStyle}>
      {BUTTONS.map((btn) => {
        const isActive = activeTypes.has(btn.type);
        const Icon = btn.icon;
        return (
          <Tappable
            key={btn.type}
            disabled={disabled}
            onClick={() => onFormat(btn.type)}
            onPointerDown={(e) => e.preventDefault()}
            onMouseDown={(e) => e.preventDefault()}
            style={{
              ...buttonBase,
              background: isActive ? 'var(--am-soft-accent-bg)' : 'transparent',
              color: isActive ? 'var(--color-accent-primary)' : 'var(--color-foreground-secondary)',
              opacity: disabled ? 0.4 : 1,
            }}
            aria-pressed={isActive}
            aria-label={t(btn.labelKey)}
          >
            <Icon style={iconStyle} />
          </Tappable>
        );
      })}
      <Tappable
        disabled={disabled}
        onClick={onLink}
        onPointerDown={(e) => e.preventDefault()}
        onMouseDown={(e) => e.preventDefault()}
        style={{
          ...buttonBase,
          background: activeTypes.has(TextEntityType.TEXT_LINK) ? 'var(--am-soft-accent-bg)' : 'transparent',
          color: activeTypes.has(TextEntityType.TEXT_LINK)
            ? 'var(--color-accent-primary)'
            : 'var(--color-foreground-secondary)',
          opacity: disabled ? 0.4 : 1,
        }}
        aria-label={t('creatives.form.formatting.link')}
      >
        <LinkIcon style={iconStyle} />
      </Tappable>
    </div>
  );
}
