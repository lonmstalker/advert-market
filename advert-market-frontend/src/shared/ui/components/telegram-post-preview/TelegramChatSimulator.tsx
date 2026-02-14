import { motion } from 'motion/react';
import type { InlineButton, MediaItem, TextEntity } from '@/shared/types/text-entity';
import { slideUp } from '../../animations';
import { ArrowRightIcon, EyeIcon } from '../../icons';
import {
  chatArea,
  chatTopBar,
  chatTopBarAvatar,
  chatTopBarSubtitle,
  chatTopBarTitle,
  postContent,
  viewCounter,
} from './styles';
import { TelegramPostBody } from './TelegramPostBody';
import { TelegramPostButtons } from './TelegramPostButtons';
import { TelegramPostHeader } from './TelegramPostHeader';
import { TelegramPostMedia } from './TelegramPostMedia';

type TelegramChatSimulatorProps = {
  text: string;
  entities: TextEntity[];
  media: MediaItem[];
  buttons: InlineButton[];
  channelTitle?: string;
  channelAvatar?: string;
  subscriberCount?: string;
};

function formatTime(): string {
  const now = new Date();
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;
}

export function TelegramChatSimulator({
  text,
  entities,
  media,
  buttons,
  channelTitle,
  channelAvatar,
  subscriberCount = '12.4K',
}: TelegramChatSimulatorProps) {
  const title = channelTitle || 'Channel';
  const initial = title.charAt(0).toUpperCase();
  const time = formatTime();

  return (
    <div style={chatArea}>
      <div style={chatTopBar}>
        <ArrowRightIcon
          style={{
            width: 18,
            height: 18,
            color: 'var(--color-accent-primary)',
            transform: 'rotate(180deg)',
            flexShrink: 0,
          }}
        />
        {channelAvatar ? (
          <img src={channelAvatar} alt={title} style={{ ...chatTopBarAvatar, objectFit: 'cover' }} />
        ) : (
          <div style={chatTopBarAvatar}>{initial}</div>
        )}
        <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
          <span style={chatTopBarTitle}>{title}</span>
          <span style={chatTopBarSubtitle}>{subscriberCount} subscribers</span>
        </div>
      </div>

      <motion.div {...slideUp} style={postContent}>
        <TelegramPostHeader channelTitle={channelTitle} channelAvatar={channelAvatar} />
        <TelegramPostMedia media={media} />
        <TelegramPostBody text={text} entities={entities} />

        <div style={viewCounter}>
          <EyeIcon style={{ width: 13, height: 13 }} />
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>1.2K</span>
          <span style={{ marginLeft: 4 }}>{time}</span>
        </div>

        <TelegramPostButtons buttons={buttons} />
      </motion.div>
    </div>
  );
}
