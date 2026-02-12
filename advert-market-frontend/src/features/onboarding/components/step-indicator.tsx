import { Icon } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { useLocation } from 'react-router';

const STEPS = ['/onboarding', '/onboarding/interest', '/onboarding/tour'];

export function StepIndicator() {
  const { pathname } = useLocation();
  const currentStep = STEPS.indexOf(pathname);

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '40px',
        padding: '0 16px',
        gap: '0',
      }}
    >
      {STEPS.map((path, i) => {
        const isCompleted = i < currentStep;
        const isActive = i === currentStep;

        return (
          <div key={path} style={{ display: 'flex', alignItems: 'center' }}>
            <motion.div
              initial={false}
              animate={{
                backgroundColor: isCompleted || isActive ? 'var(--color-accent-primary)' : 'transparent',
                borderColor: isCompleted || isActive ? 'var(--color-accent-primary)' : 'var(--color-border-separator)',
              }}
              transition={{ duration: 0.25 }}
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                border: '1.5px solid',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              {isCompleted && <Icon name="check" color="white" size="6px" />}
            </motion.div>
            {i < STEPS.length - 1 && (
              <div
                style={{
                  width: '32px',
                  height: '2px',
                  backgroundColor: 'var(--color-border-separator)',
                  position: 'relative',
                  overflow: 'hidden',
                }}
              >
                <motion.div
                  initial={false}
                  animate={{ width: i < currentStep ? '100%' : '0%' }}
                  transition={{ duration: 0.3, ease: 'easeOut' }}
                  style={{
                    height: '100%',
                    backgroundColor: 'var(--color-accent-primary)',
                  }}
                />
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
