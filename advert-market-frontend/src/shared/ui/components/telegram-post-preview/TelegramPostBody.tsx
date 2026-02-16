import type { TextEntity } from '@/shared/types/text-entity';
import { renderEntities } from './entity-renderer';

type TelegramPostBodyProps = {
  text: string;
  entities: TextEntity[];
};

export function TelegramPostBody({ text, entities }: TelegramPostBodyProps) {
  if (!text) return null;

  const nodes = renderEntities(text, entities);

  return <div className="am-tg-message-text">{nodes}</div>;
}
