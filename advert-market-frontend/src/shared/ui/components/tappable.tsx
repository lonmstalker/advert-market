import { motion } from 'motion/react';
import type { CSSProperties, MouseEventHandler, PointerEventHandler, ReactNode } from 'react';
import { tapScale } from '@/shared/ui/animations';

type TappableProps = {
  children: ReactNode;
  onClick?: () => void;
  onPointerDown?: PointerEventHandler<HTMLButtonElement>;
  onMouseDown?: MouseEventHandler<HTMLButtonElement>;
  disabled?: boolean;
  style?: CSSProperties;
  className?: string;
  'aria-label'?: string;
  'aria-pressed'?: boolean;
  'aria-expanded'?: boolean;
};

export function Tappable({
  children,
  onClick,
  onPointerDown,
  onMouseDown,
  disabled,
  style,
  className,
  ...ariaProps
}: TappableProps) {
  return (
    <motion.button
      {...tapScale}
      type="button"
      onClick={onClick}
      onPointerDown={onPointerDown}
      onMouseDown={onMouseDown}
      disabled={disabled}
      style={style}
      className={className}
      {...ariaProps}
    >
      {children}
    </motion.button>
  );
}
