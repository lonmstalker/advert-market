import { motion } from 'motion/react';
import type { CSSProperties, ReactNode } from 'react';
import { tapScale } from '@/shared/ui/animations';

type TappableProps = {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  style?: CSSProperties;
  'aria-label'?: string;
  'aria-pressed'?: boolean;
};

export function Tappable({ children, onClick, disabled, style, ...ariaProps }: TappableProps) {
  return (
    <motion.button {...tapScale} type="button" onClick={onClick} disabled={disabled} style={style} {...ariaProps}>
      {children}
    </motion.button>
  );
}
