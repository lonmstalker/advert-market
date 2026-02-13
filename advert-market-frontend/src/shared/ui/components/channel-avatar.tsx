type ChannelAvatarSize = 'xs' | 'sm' | 'md' | 'lg';

type ChannelAvatarProps = {
  title: string;
  size?: ChannelAvatarSize;
};

const sizeMap: Record<ChannelAvatarSize, { container: number; font: number }> = {
  xs: { container: 36, font: 14 },
  sm: { container: 40, font: 16 },
  md: { container: 44, font: 18 },
  lg: { container: 56, font: 22 },
};

export function computeAvatarHue(title: string): number {
  return (title.charCodeAt(0) * 37 + (title.charCodeAt(1) || 0) * 53) % 360;
}

export function ChannelAvatar({ title, size = 'md' }: ChannelAvatarProps) {
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
        flexShrink: 0,
      }}
    >
      <span style={{ color: 'var(--color-static-white)', fontSize: font, fontWeight: 600, lineHeight: 1 }}>
        {letter}
      </span>
    </div>
  );
}
