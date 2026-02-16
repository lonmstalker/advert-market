import { motion } from 'motion/react';
import type { InlineButton, MediaItem, TextEntity } from '@/shared/types/text-entity';
import { slideUp } from '../../animations';
import { EyeIcon } from '../../icons';
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
  views?: string;
  time?: string;
  edited?: boolean;
};

export function TelegramPostPreview({
  text,
  entities,
  media,
  buttons,
  channelTitle,
  channelAvatar,
  views = '1.2K',
  time = '12:40',
  edited = false,
}: TelegramPostPreviewProps) {
  const hasMedia = media.length > 0;

  return (
    <motion.div {...slideUp} className="am-tg-message-stack">
      <article className="am-tg-message-bubble">
        <svg className="am-tg-bubble-tail" viewBox="0 0 9 20" aria-hidden="true">
          <path d="M9 0v20c-4-1-7-4-9-8 5 0 8-3 9-12z" />
        </svg>
        <TelegramPostHeader channelTitle={channelTitle} channelAvatar={channelAvatar} />
        <TelegramPostMedia media={media} />
        <div className={hasMedia ? 'am-tg-message-body am-tg-message-body--with-media' : 'am-tg-message-body'}>
          <TelegramPostBody text={text} entities={entities} />
          <span className="am-tg-message-meta">
            {edited ? <span className="am-tg-message-edited">edited</span> : null}
            <span>{time}</span>
            <span className="am-tg-message-views">
              <EyeIcon className="am-tg-message-eye" />
              <span>{views}</span>
            </span>
          </span>
        </div>
      </article>
      <TelegramPostButtons buttons={buttons} />
    </motion.div>
  );
}
