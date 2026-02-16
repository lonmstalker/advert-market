import { Image } from '@telegram-tools/ui-kit';

type TelegramPostHeaderProps = {
  channelTitle?: string;
  channelAvatar?: string;
};

export function TelegramPostHeader({ channelTitle, channelAvatar }: TelegramPostHeaderProps) {
  const title = channelTitle || 'Channel';
  const initial = title.charAt(0).toUpperCase();

  return (
    <div className="am-tg-channel-header">
      {channelAvatar ? (
        <Image
          src={channelAvatar}
          alt={title}
          className="am-tg-channel-avatar"
          width="24px"
          height="24px"
          borderRadius="999px"
        />
      ) : (
        <div className="am-tg-channel-avatar am-tg-channel-avatar--fallback">{initial}</div>
      )}
      <span className="am-tg-channel-name">{title}</span>
    </div>
  );
}
