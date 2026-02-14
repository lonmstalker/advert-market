import { type ReactNode, useCallback, useState } from 'react';
import { spoilerHidden, spoilerRevealed } from './styles';

type TelegramSpoilerProps = {
  children: ReactNode;
};

export function TelegramSpoiler({ children }: TelegramSpoilerProps) {
  const [revealed, setRevealed] = useState(false);

  const handleClick = useCallback(() => {
    setRevealed(true);
  }, []);

  return (
    <button
      type="button"
      style={revealed ? spoilerRevealed : spoilerHidden}
      onClick={handleClick}
      aria-label={revealed ? undefined : 'Spoiler (click to reveal)'}
    >
      {children}
    </button>
  );
}
