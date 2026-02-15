import type { LucideIcon } from 'lucide-react';
import type { CSSProperties } from 'react';

type AppIconProps = {
  icon: LucideIcon;
  size?: number;
  className?: string;
  style?: CSSProperties;
};

export function AppIcon({ icon: Icon, size = 20, className, style }: AppIconProps) {
  return <Icon size={size} className={className} style={style} aria-hidden="true" />;
}
