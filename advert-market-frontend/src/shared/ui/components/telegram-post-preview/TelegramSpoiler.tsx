import { Button } from '@telegram-tools/ui-kit';
import { type ReactNode, useCallback, useState } from 'react';

type TelegramSpoilerProps = {
  children: ReactNode;
};

export function TelegramSpoiler({ children }: TelegramSpoilerProps) {
  const [revealed, setRevealed] = useState(false);

  const handleClick = useCallback(() => {
    setRevealed(true);
  }, []);

  return (
    <Button
      type="secondary"
      className={revealed ? 'am-tg-spoiler am-tg-spoiler--revealed' : 'am-tg-spoiler'}
      onClick={handleClick}
      icon={<span className="am-tg-spoiler__content">{children}</span>}
    />
  );
}
