import { motion } from 'motion/react';
import type { CSSProperties, MouseEventHandler, PointerEventHandler, ReactNode, TouchEventHandler } from 'react';
import { pressScale } from '@/shared/ui/animations';

function joinClasses(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}

type TappableProps = {
  children: ReactNode;
  onClick?: () => void;
  onPointerDown?: PointerEventHandler<HTMLButtonElement>;
  onMouseDown?: MouseEventHandler<HTMLButtonElement>;
  onTouchStart?: TouchEventHandler<HTMLButtonElement>;
  disabled?: boolean;
  style?: CSSProperties;
  className?: string;
  'aria-label'?: string;
  'aria-pressed'?: boolean;
  'aria-expanded'?: boolean;
  'data-testid'?: string;
  'data-active'?: 'true' | 'false';
};

export function Tappable({
  children,
  onClick,
  onPointerDown,
  onMouseDown,
  onTouchStart,
  disabled,
  style,
  className,
  ...ariaProps
}: TappableProps) {
  return (
    <motion.button
      {...pressScale}
      type="button"
      onClick={onClick}
      onPointerDown={onPointerDown}
      onMouseDown={onMouseDown}
      onTouchStart={onTouchStart}
      disabled={disabled}
      style={{ minWidth: 44, minHeight: 44, ...style }}
      className={joinClasses('am-tappable', className, disabled && 'opacity-50 pointer-events-none')}
      {...ariaProps}
    >
      {children}
    </motion.button>
  );
}
