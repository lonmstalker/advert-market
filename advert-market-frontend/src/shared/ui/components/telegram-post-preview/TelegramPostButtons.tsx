import type { InlineButton } from '@/shared/types/text-entity';
import { GlobeIcon } from '../../icons';

type TelegramPostButtonsProps = {
  buttons: InlineButton[];
};

export function TelegramPostButtons({ buttons }: TelegramPostButtonsProps) {
  if (buttons.length === 0) return null;

  const rows: InlineButton[][] = [];
  for (let i = 0; i < buttons.length; i += 2) {
    rows.push(buttons.slice(i, i + 2));
  }

  return (
    <div className="am-tg-inline-keyboard">
      {rows.map((row) => (
        <div key={row.map((b) => b.url).join(',')} className="am-tg-inline-keyboard-row">
          {row.map((btn) => (
            <span key={btn.url} className="am-tg-inline-button">
              {btn.text}
              <GlobeIcon className="am-tg-inline-icon" />
            </span>
          ))}
        </div>
      ))}
    </div>
  );
}
