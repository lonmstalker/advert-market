import { headerAvatar, headerInfo, headerTime, headerTitle, postHeader } from './styles';

type TelegramPostHeaderProps = {
  channelTitle?: string;
  channelAvatar?: string;
  time?: string;
};

export function TelegramPostHeader({ channelTitle, channelAvatar, time }: TelegramPostHeaderProps) {
  const title = channelTitle || 'Channel';
  const initial = title.charAt(0).toUpperCase();

  return (
    <div style={postHeader}>
      {channelAvatar ? (
        <img src={channelAvatar} alt={title} style={{ ...headerAvatar, objectFit: 'cover' }} />
      ) : (
        <div style={headerAvatar}>{initial}</div>
      )}
      <div style={headerInfo}>
        <span style={headerTitle}>{title}</span>
        <span style={headerTime}>{time ?? '12:00'}</span>
      </div>
    </div>
  );
}
