import type { ReactNode } from 'react';

type MockupContainerProps = {
  children: ReactNode;
};

export function MockupContainer({ children }: MockupContainerProps) {
  return (
    <div data-testid="onboarding-mockup-container" className="am-onboarding-mockup">
      {children}
    </div>
  );
}
