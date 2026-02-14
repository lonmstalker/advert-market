import type { TextEntity } from '@/shared/types/text-entity';
import { renderEntities } from './entity-renderer';
import { postBody } from './styles';

type TelegramPostBodyProps = {
  text: string;
  entities: TextEntity[];
};

export function TelegramPostBody({ text, entities }: TelegramPostBodyProps) {
  if (!text) return null;

  const nodes = renderEntities(text, entities);

  return <div style={postBody}>{nodes}</div>;
}
