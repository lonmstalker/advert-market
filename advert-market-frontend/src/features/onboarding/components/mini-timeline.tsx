import { Icon, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { Tappable } from '@/shared/ui';

function joinClasses(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}

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
    <ul aria-label={t('onboarding.tour.mockup.dealTimeline')} className="am-mini-timeline">
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
          <li key={step.label} className="am-mini-timeline__item">
            <div className="am-mini-timeline__rail">
              {isCompleted ? (
                <div aria-hidden="true" className="am-mini-timeline__dot am-mini-timeline__dot--completed">
                  <Icon name="check" size="10px" className="am-icon-white" />
                </div>
              ) : isActive ? (
                <motion.div
                  aria-hidden="true"
                  animate={{ opacity: [1, 0.72, 1], scale: [1, 0.96, 1] }}
                  transition={{ duration: 1.2, repeat: 2, ease: 'easeInOut' }}
                  className="am-mini-timeline__dot am-mini-timeline__dot--active"
                />
              ) : (
                <div aria-hidden="true" className="am-mini-timeline__dot am-mini-timeline__dot--pending" />
              )}
              {!isLast && (
                <div
                  className={joinClasses('am-mini-timeline__line', isCompleted && 'am-mini-timeline__line--completed')}
                />
              )}
            </div>

            <div className="am-mini-timeline__content">
              <Tappable
                className={joinClasses(
                  'am-mini-timeline__step focusable',
                  isActive && 'am-mini-timeline__step--active',
                  isClickable && 'am-mini-timeline__step--clickable',
                  isExpanded && 'am-mini-timeline__step--expanded',
                )}
                aria-expanded={isExpanded}
                aria-label={`${step.label} — ${statusLabels[step.status] ?? step.status}`}
                onClick={isClickable ? handleClick : undefined}
              >
                <Text
                  type="caption1"
                  color={isCompleted || isActive ? 'primary' : 'secondary'}
                  weight={isActive ? 'medium' : 'regular'}
                >
                  {step.label}
                </Text>
              </Tappable>

              <AnimatePresence>
                {isExpanded && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.25, ease: 'easeOut' }}
                    className="am-mini-timeline__details"
                  >
                    <div className="am-mini-timeline__details-inner">
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
