import type { InlineButton } from '@/shared/types/text-entity';
import { GlobeIcon } from '../../icons';
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
          <GlobeIcon style={{ width: 12, height: 12, opacity: 0.7 }} />
        </span>
      ))}
    </div>
  );
}
