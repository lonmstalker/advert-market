import type { InlineButton } from '@/shared/types/text-entity';
import { buttonsContainer, inlineButton } from './styles';

type TelegramPostButtonsProps = {
  buttons: InlineButton[];
};

export function TelegramPostButtons({ buttons }: TelegramPostButtonsProps) {
  if (buttons.length === 0) return null;

  return (
    <div style={buttonsContainer}>
      {buttons.map((btn, index) => (
        <span key={`${btn.url}-${index}`} style={inlineButton}>
          {btn.text}
        </span>
      ))}
    </div>
  );
}
