import type { ComponentType, SVGProps } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
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
  const haptic = useHaptic();

  function handleFormat(type: TextEntityType) {
    haptic.impactOccurred('light');
    onFormat(type);
  }

  function handleLink() {
    haptic.impactOccurred('light');
    onLink();
  }

  return (
    <div className="am-formatting-toolbar">
      {BUTTONS.map((btn) => {
        const isActive = activeTypes.has(btn.type);
        const Icon = btn.icon;
        return (
          <Tappable
            key={btn.type}
            disabled={disabled}
            onClick={() => handleFormat(btn.type)}
            onPointerDown={(e) => e.preventDefault()}
            onMouseDown={(e) => e.preventDefault()}
            className="am-formatting-toolbar__button"
            data-active={isActive ? 'true' : 'false'}
            data-disabled={disabled ? 'true' : 'false'}
            aria-pressed={isActive}
            aria-label={t(btn.labelKey)}
          >
            <Icon className="am-formatting-toolbar__icon" />
          </Tappable>
        );
      })}
      <Tappable
        disabled={disabled}
        onClick={handleLink}
        onPointerDown={(e) => e.preventDefault()}
        onMouseDown={(e) => e.preventDefault()}
        className="am-formatting-toolbar__button"
        data-active={activeTypes.has(TextEntityType.TEXT_LINK) ? 'true' : 'false'}
        data-disabled={disabled ? 'true' : 'false'}
        aria-label={t('creatives.form.formatting.link')}
      >
        <LinkIcon className="am-formatting-toolbar__icon" />
      </Tappable>
    </div>
  );
}
