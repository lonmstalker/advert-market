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
  xl: { container: 48, font: 20 },
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
      style={{
        width: container,
        height: container,
        borderRadius: '50%',
        background: `hsl(${hue}, 55%, 55%)`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <span style={{ color: 'var(--color-static-white)', fontSize: font, fontWeight: 600, lineHeight: 1 }}>
        {letter}
      </span>
    </div>
  );
}

function BadgeOverlay({ badge }: { badge: ReactNode }) {
  return (
    <div
      style={{
        position: 'absolute',
        bottom: -2,
        right: -2,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {badge}
    </div>
  );
}

export function ChannelAvatar({ title, size = 'md', badge, onClick }: ChannelAvatarProps) {
  const { container } = sizeMap[size];
  const wrapperStyle = { position: 'relative' as const, width: container, height: container, flexShrink: 0 };

  if (onClick) {
    return (
      <button
        type="button"
        onClick={onClick}
        style={{ ...wrapperStyle, cursor: 'pointer', background: 'none', border: 'none', padding: 0 }}
      >
        <AvatarContent title={title} size={size} />
        {badge && <BadgeOverlay badge={badge} />}
      </button>
    );
  }

  return (
    <div style={wrapperStyle}>
      <AvatarContent title={title} size={size} />
      {badge && <BadgeOverlay badge={badge} />}
    </div>
  );
}
