import { Text } from '@telegram-tools/ui-kit';
import { Tappable } from '@/shared/ui';

type MockupTextButtonProps = {
  text: string;
  color?: 'accent' | 'secondary';
  onClick: () => void;
};

export function MockupTextButton({ text, color = 'secondary', onClick }: MockupTextButtonProps) {
  return (
    <Tappable
      data-testid="onboarding-mockup-text-button"
      className="am-onboarding-mockup-text-button focusable"
      onClick={onClick}
    >
      <Text type="caption1" color={color}>
        {text}
      </Text>
    </Tappable>
  );
}
