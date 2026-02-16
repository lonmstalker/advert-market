import type { ReactNode } from 'react';

type ChannelAvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

type ChannelAvatarProps = {
  title: string;
  size?: ChannelAvatarSize;
  badge?: ReactNode;
  onClick?: () => void;
};

const sizeMap: Record<ChannelAvatarSize, { container: number; font: number }> = {
  xs: { container: 36, font: 14 },
  sm: { container: 40, font: 16 },
  md: { container: 44, font: 18 },
  lg: { container: 56, font: 22 },
  xl: { container: 64, font: 26 },
};

export function computeAvatarHue(title: string): number {
  return (title.charCodeAt(0) * 37 + (title.charCodeAt(1) || 0) * 53) % 360;
}

function AvatarContent({ title, size }: { title: string; size: ChannelAvatarSize }) {
  const letter = title.charAt(0).toUpperCase();
  const hue = computeAvatarHue(title);
  const { container, font } = sizeMap[size];

  return (
    <div
      className="am-avatar"
      style={{ width: container, height: container, background: `hsl(${hue}, 55%, 55%)` }}
    >
      <span className="am-avatar__letter" style={{ fontSize: font }}>
        {letter}
      </span>
    </div>
  );
}

function BadgeOverlay({ badge }: { badge: ReactNode }) {
  return <div className="am-avatar__badge">{badge}</div>;
}

export function ChannelAvatar({ title, size = 'md', badge, onClick }: ChannelAvatarProps) {
  const { container } = sizeMap[size];
  const Tag = onClick ? 'button' : 'div';

  return (
    <Tag
      {...(onClick ? { type: 'button' as const, onClick } : {})}
      className="am-avatar__wrapper"
      style={{ width: container, height: container }}
    >
      <AvatarContent title={title} size={size} />
      {badge && <BadgeOverlay badge={badge} />}
    </Tag>
  );
}
