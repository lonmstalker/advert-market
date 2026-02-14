import { motion } from 'motion/react';
import type { InlineButton, MediaItem, TextEntity } from '@/shared/types/text-entity';
import { slideUp } from '../../animations';
import { postContent } from './styles';
import { TelegramPostBody } from './TelegramPostBody';
import { TelegramPostButtons } from './TelegramPostButtons';
import { TelegramPostHeader } from './TelegramPostHeader';
import { TelegramPostMedia } from './TelegramPostMedia';

export type TelegramPostPreviewProps = {
  text: string;
  entities: TextEntity[];
  media: MediaItem[];
  buttons: InlineButton[];
  channelTitle?: string;
  channelAvatar?: string;
};

export function TelegramPostPreview({
  text,
  entities,
  media,
  buttons,
  channelTitle,
  channelAvatar,
}: TelegramPostPreviewProps) {
  return (
    <motion.div {...slideUp} style={postContent}>
      <TelegramPostHeader channelTitle={channelTitle} channelAvatar={channelAvatar} />
      <TelegramPostMedia media={media} />
      <TelegramPostBody text={text} entities={entities} />
      <TelegramPostButtons buttons={buttons} />
    </motion.div>
  );
}
