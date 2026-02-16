import { Icon, Text } from '@telegram-tools/ui-kit';
import { easeOut } from 'motion';
import { AnimatePresence, motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Tappable } from '@/shared/ui';
import type { TimelineStep } from '../lib/deal-status';

type DealTimelineProps = {
  steps: TimelineStep[];
};

const VISIBLE_PENDING = 2;

function joinClasses(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}

function formatTimelineDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short' });
}

export function DealTimeline({ steps }: DealTimelineProps) {
  const { t } = useTranslation();
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null);
  const [showAllPending, setShowAllPending] = useState(false);

  const nonPendingSteps = steps.filter((s) => s.state !== 'pending');
  const pendingSteps = steps.filter((s) => s.state === 'pending');

  const visiblePending = showAllPending ? pendingSteps : pendingSteps.slice(0, VISIBLE_PENDING);
  const collapsedCount = pendingSteps.length - VISIBLE_PENDING;
  const showExpandButton = !showAllPending && collapsedCount > 0;

  const visibleSteps = [...nonPendingSteps, ...visiblePending];

  return (
    <div className="px-4">
      <div className="mb-3">
        <Text type="subheadline2" weight="bold">
          {t('deals.detail.timeline')}
        </Text>
      </div>
      <ul aria-label={t('deals.detail.timeline')} className="flex flex-col gap-1 list-none m-0 p-0">
        {visibleSteps.map((step, i) => {
          const globalIndex = steps.indexOf(step);
          const isActive = step.state === 'active';
          const isCompleted = step.state === 'completed';
          const isPending = step.state === 'pending';
          const isLast = i === visibleSteps.length - 1 && !showExpandButton;
          const isExpanded = expandedIndex === globalIndex && !!step.description;

          return (
            <li key={`${step.status}-${globalIndex}`} className="flex items-stretch min-h-9">
              {/* Node column */}
              <div className="flex flex-col items-center w-7 shrink-0">
                {isCompleted ? (
                  <div aria-hidden="true" className="w-4 h-4 rounded-full bg-accent flex items-center justify-center mt-0.5">
                    <Icon name="check" size="10px" className="am-icon-white" />
                  </div>
                ) : isActive ? (
                  <motion.div
                    aria-hidden="true"
                    animate={{
                      boxShadow: [
                        '0 0 0 0px color-mix(in srgb, var(--color-accent-primary) 30%, transparent)',
                        '0 0 0 6px color-mix(in srgb, var(--color-accent-primary) 0%, transparent)',
                        '0 0 0 0px color-mix(in srgb, var(--color-accent-primary) 30%, transparent)',
                      ],
                    }}
                    transition={{ duration: 2, repeat: Number.POSITIVE_INFINITY }}
                    className="w-5 h-5 rounded-full border-[2.5px] border-accent bg-bg-base am-timeline-glow"
                  />
                ) : (
                  <div aria-hidden="true" className="w-3 h-3 rounded-full border-[1.5px] border-separator mt-1" />
                )}
                {!isLast && (
                  <div
                    className={joinClasses(
                      'flex-1 min-h-2',
                      isCompleted ? 'w-0.5 bg-accent' : 'w-[1.5px] bg-separator',
                    )}
                  />
                )}
              </div>

              {/* Content column */}
              <div className={joinClasses('flex-1 min-w-0', !isLast && 'pb-2')}>
                <Tappable
                  onClick={step.description ? () => setExpandedIndex(isExpanded ? null : globalIndex) : undefined}
                  className={joinClasses(
                    'flex items-center justify-between w-full border-none text-left [-webkit-tap-highlight-color:transparent]',
                    isActive ? 'py-1 px-2 bg-soft-accent rounded-lg' : 'pl-2 bg-transparent',
                    step.description ? 'cursor-pointer' : 'cursor-default',
                  )}
                >
                  <Text
                    type={isActive ? 'body' : 'subheadline2'}
                    color={isPending ? 'secondary' : 'primary'}
                    weight={isActive ? 'medium' : 'regular'}
                  >
                    {step.label}
                  </Text>
                  {isCompleted && step.timestamp && (
                    <span className="shrink-0 ml-2">
                      <Text type="caption1" color="secondary">
                        {formatTimelineDate(step.timestamp)}
                      </Text>
                    </span>
                  )}
                </Tappable>

                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.25, ease: easeOut }}
                      className="overflow-hidden pl-2"
                    >
                      <div className="pb-1 pt-0.5">
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

      {showExpandButton && (
        <Tappable
          onClick={() => setShowAllPending(true)}
          className="block bg-transparent border-none cursor-pointer pt-1.5 pl-9 text-[13px] font-medium text-accent [-webkit-tap-highlight-color:transparent]"
        >
          {t('deals.detail.moreSteps', { count: collapsedCount })}
        </Tappable>
      )}
    </div>
  );
}
