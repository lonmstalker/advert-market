import { Icon, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useTranslation } from 'react-i18next';

type TimelineStep = {
  label: string;
  status: 'completed' | 'active' | 'pending';
  description?: string;
};

type MiniTimelineProps = {
  steps: TimelineStep[];
  onActiveClick?: () => void;
  expandedIndex?: number | null;
  onStepClick?: (index: number) => void;
};

export function MiniTimeline({ steps, onActiveClick, expandedIndex, onStepClick }: MiniTimelineProps) {
  const { t } = useTranslation();

  const statusLabels: Record<string, string> = {
    completed: t('onboarding.tour.mockup.statusCompleted'),
    active: t('onboarding.tour.mockup.statusActive'),
    pending: t('onboarding.tour.mockup.statusPending'),
  };

  return (
    <ul
      aria-label={t('onboarding.tour.mockup.dealTimeline')}
      style={{ display: 'flex', flexDirection: 'column', gap: '0', padding: '8px 0', listStyle: 'none', margin: 0 }}
    >
      {steps.map((step, i) => {
        const isActive = step.status === 'active';
        const isCompleted = step.status === 'completed';
        const isLast = i === steps.length - 1;
        const isExpanded = expandedIndex === i && !!step.description;
        const isClickable = !!step.description || isActive;

        function handleClick() {
          if (isActive && onActiveClick) {
            onActiveClick();
          }
          if (step.description && onStepClick) {
            onStepClick(i);
          }
        }

        return (
          <li key={step.label} style={{ display: 'flex', alignItems: 'stretch', minHeight: '32px' }}>
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                width: '24px',
                flexShrink: 0,
              }}
            >
              {isCompleted ? (
                <div
                  aria-hidden="true"
                  style={{
                    width: '16px',
                    height: '16px',
                    borderRadius: '50%',
                    backgroundColor: 'var(--color-accent-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginTop: '2px',
                  }}
                >
                  <Icon name="check" color="white" size="10px" />
                </div>
              ) : isActive ? (
                <motion.div
                  aria-hidden="true"
                  animate={{
                    boxShadow: [
                      '0 0 0 0px rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.3)',
                      '0 0 0 4px rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0)',
                      '0 0 0 0px rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.3)',
                    ],
                  }}
                  transition={{ duration: 2, repeat: Number.POSITIVE_INFINITY }}
                  style={{
                    width: '16px',
                    height: '16px',
                    borderRadius: '50%',
                    border: '2px solid var(--color-accent-primary)',
                    backgroundColor: 'var(--color-background-base)',
                    marginTop: '2px',
                  }}
                />
              ) : (
                <div
                  aria-hidden="true"
                  style={{
                    width: '16px',
                    height: '16px',
                    borderRadius: '50%',
                    border: '1.5px solid var(--color-border-separator)',
                    marginTop: '2px',
                  }}
                />
              )}
              {!isLast && (
                <div
                  style={{
                    width: '1.5px',
                    flex: 1,
                    backgroundColor: isCompleted ? 'var(--color-accent-primary)' : 'var(--color-border-separator)',
                    minHeight: '8px',
                  }}
                />
              )}
            </div>

            <div style={{ flex: 1, minWidth: 0 }}>
              <button
                type="button"
                className="focusable"
                aria-expanded={isExpanded}
                aria-label={`${step.label} — ${statusLabels[step.status] ?? step.status}`}
                onClick={isClickable ? handleClick : undefined}
                style={{
                  display: 'block',
                  width: '100%',
                  padding: isActive ? '2px 8px 2px 8px' : '0 0 0 8px',
                  cursor: isClickable ? 'pointer' : 'default',
                  background: isActive ? 'rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.08)' : 'none',
                  border: 'none',
                  borderRadius: isActive ? '8px' : '0',
                  textAlign: 'left',
                  WebkitTapHighlightColor: 'transparent',
                  marginBottom: isExpanded ? '0' : '8px',
                }}
              >
                <Text
                  type="caption1"
                  color={isCompleted || isActive ? 'primary' : 'secondary'}
                  weight={isActive ? 'medium' : 'regular'}
                >
                  {step.label}
                </Text>
              </button>

              <AnimatePresence>
                {isExpanded && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.25, ease: 'easeOut' }}
                    style={{ overflow: 'hidden', paddingLeft: '8px' }}
                  >
                    <div style={{ paddingBottom: '8px', paddingTop: '2px' }}>
                      <Text type="caption1" color="secondary">
                        {step.description}
                      </Text>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
