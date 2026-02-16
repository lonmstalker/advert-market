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
    <button
      type="button"
      className={revealed ? 'am-tg-spoiler am-tg-spoiler--revealed' : 'am-tg-spoiler'}
      onClick={handleClick}
    >
      <span className="am-tg-spoiler__content">{children}</span>
    </button>
  );
}
