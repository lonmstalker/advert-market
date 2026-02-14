import { motion } from 'motion/react';
import type { InlineButton } from '@/shared/types/text-entity';
import { pressScale } from '../../animations';
import { buttonsContainer, inlineButton } from './styles';

type TelegramPostButtonsProps = {
  buttons: InlineButton[];
};

export function TelegramPostButtons({ buttons }: TelegramPostButtonsProps) {
  if (buttons.length === 0) return null;

  return (
    <div style={buttonsContainer}>
      {buttons.map((btn, index) => (
        <motion.a
          key={`${btn.url}-${index}`}
          href={btn.url}
          target="_blank"
          rel="noopener noreferrer"
          style={inlineButton}
          {...pressScale}
        >
          {btn.text}
        </motion.a>
      ))}
    </div>
  );
}
