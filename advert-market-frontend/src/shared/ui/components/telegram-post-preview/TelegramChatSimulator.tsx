import { Image } from '@telegram-tools/ui-kit';
import type { InlineButton, MediaItem, TextEntity } from '@/shared/types/text-entity';
import { ArrowRightIcon, EyeIcon } from '../../icons';
import { TelegramPostPreview } from './TelegramPostPreview';

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
    <div className="am-tg-chat-window">
      <div className="am-tg-chat-pattern" />
      <div className="am-tg-chat-content">
        <div className="am-tg-sim-header">
          <ArrowRightIcon className="am-tg-sim-back" />
          {channelAvatar ? (
            <Image
              src={channelAvatar}
              alt={title}
              className="am-tg-sim-avatar"
              width="28px"
              height="28px"
              borderRadius="999px"
            />
          ) : (
            <div className="am-tg-sim-avatar am-tg-sim-avatar--fallback">{initial}</div>
          )}
          <div className="am-tg-sim-header-copy">
            <span className="am-tg-sim-title">{title}</span>
            <span className="am-tg-sim-subtitle">{subscriberCount} subscribers</span>
          </div>
          <EyeIcon className="am-tg-sim-eye" />
        </div>

        <div className="am-tg-day-divider">Today</div>

        <TelegramPostPreview
          text={text}
          entities={entities}
          media={media}
          buttons={buttons}
          channelTitle={channelTitle}
          channelAvatar={channelAvatar}
          time={time}
          views="1.2K"
        />
      </div>
    </div>
  );
}
