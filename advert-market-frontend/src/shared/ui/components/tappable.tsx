import { motion } from 'motion/react';
import type { CSSProperties, ReactNode } from 'react';
import { tapScale } from '@/shared/ui/animations';

type TappableProps = {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  style?: CSSProperties;
  className?: string;
  'aria-label'?: string;
  'aria-pressed'?: boolean;
  'aria-expanded'?: boolean;
};

export function Tappable({ children, onClick, disabled, style, className, ...ariaProps }: TappableProps) {
  return (
    <motion.button
      {...tapScale}
      type="button"
      onClick={onClick}
      disabled={disabled}
      style={style}
      className={className}
      {...ariaProps}
    >
      {children}
    </motion.button>
  );
}
