import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '@/test/test-utils';
import { StepIndicator } from './step-indicator';

// Mock motion/react so motion.div renders as a plain div
// with `animate` object spread into `style` for testability in jsdom.
vi.mock('motion/react', () => {
  const React = require('react');

  function createMotionComponent(tag: string) {
    return React.forwardRef((props: Record<string, unknown>, ref: unknown) => {
      const { animate, initial, transition, whileHover, whileTap, ...rest } = props;
      const animateStyles = typeof animate === 'object' && animate !== null ? animate : {};
      const mergedStyle = { ...(rest.style as object), ...animateStyles };
      return React.createElement(tag, { ...rest, style: mergedStyle, ref });
    });
  }

  const motionProxy = new Proxy(
    {},
    {
      get(_target: object, prop: string) {
        return createMotionComponent(prop);
      },
    },
  );

  return {
    motion: motionProxy,
    AnimatePresence: ({ children }: { children: React.ReactNode }) => children,
  };
});

function getStepDots(container: HTMLElement) {
  return Array.from(container.querySelectorAll('[data-testid="step-dot"]'));
}

function getConnectors(container: HTMLElement) {
  return Array.from(container.querySelectorAll('[data-testid="step-connector"]'));
}

function getDotBgColor(dot: Element): string {
  return (dot as HTMLElement).style.backgroundColor;
}

describe('StepIndicator', () => {
  it('renders 3 step dots', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/onboarding'],
    });
    expect(getStepDots(container)).toHaveLength(3);
  });

  it('renders 2 connector lines between steps', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/onboarding'],
    });
    expect(getConnectors(container)).toHaveLength(2);
  });

  it('highlights only the first dot on /onboarding (step 0)', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/onboarding'],
    });
    const dots = getStepDots(container);
    expect(getDotBgColor(dots[0])).toBe('var(--color-accent-primary)');
    expect(getDotBgColor(dots[1])).toBe('transparent');
    expect(getDotBgColor(dots[2])).toBe('transparent');
  });

  it('highlights first two dots on /onboarding/interest (step 1)', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/onboarding/interest'],
    });
    const dots = getStepDots(container);
    expect(getDotBgColor(dots[0])).toBe('var(--color-accent-primary)');
    expect(getDotBgColor(dots[1])).toBe('var(--color-accent-primary)');
    expect(getDotBgColor(dots[2])).toBe('transparent');
  });

  it('highlights all three dots on /onboarding/tour (step 2)', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/onboarding/tour'],
    });
    const dots = getStepDots(container);
    expect(getDotBgColor(dots[0])).toBe('var(--color-accent-primary)');
    expect(getDotBgColor(dots[1])).toBe('var(--color-accent-primary)');
    expect(getDotBgColor(dots[2])).toBe('var(--color-accent-primary)');
  });

  it('shows no dots active on an unknown route (currentStep = -1)', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/some-other-route'],
    });
    const dots = getStepDots(container);
    expect(getDotBgColor(dots[0])).toBe('transparent');
    expect(getDotBgColor(dots[1])).toBe('transparent');
    expect(getDotBgColor(dots[2])).toBe('transparent');
  });

  it('all connectors progress bars are filled on the last step', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/onboarding/tour'],
    });
    const connectors = getConnectors(container);
    const inner1 = connectors[0]?.querySelector('div') as HTMLElement;
    const inner2 = connectors[1]?.querySelector('div') as HTMLElement;
    expect(inner1.style.width).toBe('100%');
    expect(inner2.style.width).toBe('100%');
  });

  it('all connectors progress bars are empty on the first step', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/onboarding'],
    });
    const connectors = getConnectors(container);
    const inner1 = connectors[0]?.querySelector('div') as HTMLElement;
    const inner2 = connectors[1]?.querySelector('div') as HTMLElement;
    expect(inner1.style.width).toBe('0%');
    expect(inner2.style.width).toBe('0%');
  });

  it('first connector filled and second empty on the middle step', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/onboarding/interest'],
    });
    const connectors = getConnectors(container);
    const inner1 = connectors[0]?.querySelector('div') as HTMLElement;
    const inner2 = connectors[1]?.querySelector('div') as HTMLElement;
    expect(inner1.style.width).toBe('100%');
    expect(inner2.style.width).toBe('0%');
  });

  it('sets border color to accent for active/completed dots', () => {
    const { container } = renderWithProviders(<StepIndicator />, {
      initialEntries: ['/onboarding/interest'],
    });
    const dots = getStepDots(container);
    expect((dots[0] as HTMLElement).style.borderColor).toBe('var(--color-accent-primary)');
    expect((dots[1] as HTMLElement).style.borderColor).toBe('var(--color-accent-primary)');
    expect((dots[2] as HTMLElement).style.borderColor).toBe('var(--color-border-separator)');
  });
});
