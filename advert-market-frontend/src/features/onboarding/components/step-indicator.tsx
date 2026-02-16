import { Icon } from '@telegram-tools/ui-kit';
import { easeOut } from 'motion';
import { motion } from 'motion/react';
import { useLocation } from 'react-router';

const STEPS = ['/onboarding', '/onboarding/interest', '/onboarding/tour'];

export function StepIndicator() {
  const { pathname } = useLocation();
  const currentStep = STEPS.indexOf(pathname);

  return (
    <div className="flex items-center justify-center h-10 px-4">
      {STEPS.map((path, i) => {
        const isCompleted = i < currentStep;
        const isActive = i === currentStep;

        return (
          <div key={path} className="flex items-center">
            <motion.div
              data-testid="step-dot"
              initial={false}
              animate={{
                backgroundColor: isCompleted || isActive ? 'var(--color-accent-primary)' : 'transparent',
                borderColor: isCompleted || isActive ? 'var(--color-accent-primary)' : 'var(--color-border-separator)',
              }}
              transition={{ duration: 0.25 }}
              className="size-2 rounded-full border-[1.5px] flex items-center justify-center shrink-0"
            >
              {isCompleted && <Icon name="check" size="6px" className="am-icon-white" />}
            </motion.div>
            {i < STEPS.length - 1 && (
              <div data-testid="step-connector" className="w-8 h-0.5 bg-separator relative overflow-hidden">
                <motion.div
                  initial={false}
                  animate={{ width: i < currentStep ? '100%' : '0%' }}
                  transition={{ duration: 0.3, ease: easeOut }}
                  className="h-full bg-accent"
                />
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
