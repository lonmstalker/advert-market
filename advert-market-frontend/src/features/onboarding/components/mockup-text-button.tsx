import { Text } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';

type MockupTextButtonProps = {
  text: string;
  color?: 'accent' | 'secondary';
  onClick: () => void;
};

export function MockupTextButton({ text, color = 'secondary', onClick }: MockupTextButtonProps) {
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.97 }}
      onClick={onClick}
      style={{
        display: 'block',
        width: '100%',
        textAlign: 'center',
        padding: '8px',
        cursor: 'pointer',
        WebkitTapHighlightColor: 'transparent',
        background: 'none',
        border: 'none',
        outline: 'none',
      }}
    >
      <Text type="caption1" color={color}>
        {text}
      </Text>
    </motion.button>
  );
}
