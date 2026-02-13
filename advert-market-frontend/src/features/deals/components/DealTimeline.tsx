import { Icon, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TimelineStep } from '../lib/deal-status';

type DealTimelineProps = {
  steps: TimelineStep[];
};

const VISIBLE_PENDING = 2;

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
    <div style={{ padding: '0 16px' }}>
      <Text type="subheadline2" weight="bold" style={{ marginBottom: 12 }}>
        {t('deals.detail.timeline')}
      </Text>
      <ul
        aria-label={t('deals.detail.timeline')}
        style={{ display: 'flex', flexDirection: 'column', gap: 0, padding: 0, listStyle: 'none', margin: 0 }}
      >
        {visibleSteps.map((step, i) => {
          const globalIndex = steps.indexOf(step);
          const isActive = step.state === 'active';
          const isCompleted = step.state === 'completed';
          const isPending = step.state === 'pending';
          const isLast = i === visibleSteps.length - 1 && !showExpandButton;
          const isExpanded = expandedIndex === globalIndex && !!step.description;

          const nodeSize = isActive ? 20 : isCompleted ? 16 : 12;

          return (
            <li key={`${step.status}-${globalIndex}`} style={{ display: 'flex', alignItems: 'stretch', minHeight: 36 }}>
              {/* Node column */}
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  width: 28,
                  flexShrink: 0,
                }}
              >
                {isCompleted ? (
                  <div
                    aria-hidden="true"
                    style={{
                      width: nodeSize,
                      height: nodeSize,
                      borderRadius: '50%',
                      backgroundColor: 'var(--color-accent-primary)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      marginTop: 2,
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
                        '0 0 0 5px rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0)',
                        '0 0 0 0px rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.3)',
                      ],
                    }}
                    transition={{ duration: 2, repeat: Number.POSITIVE_INFINITY }}
                    style={{
                      width: nodeSize,
                      height: nodeSize,
                      borderRadius: '50%',
                      border: '2.5px solid var(--color-accent-primary)',
                      backgroundColor: 'var(--color-background-base)',
                      marginTop: 0,
                    }}
                  />
                ) : (
                  <div
                    aria-hidden="true"
                    style={{
                      width: nodeSize,
                      height: nodeSize,
                      borderRadius: '50%',
                      border: '1.5px solid var(--color-border-separator)',
                      marginTop: 4,
                    }}
                  />
                )}
                {!isLast && (
                  <div
                    style={{
                      width: isCompleted ? 2 : 1.5,
                      flex: 1,
                      backgroundColor: isCompleted ? 'var(--color-accent-primary)' : 'var(--color-border-separator)',
                      minHeight: 8,
                    }}
                  />
                )}
              </div>

              {/* Content column */}
              <div style={{ flex: 1, minWidth: 0, paddingBottom: isLast ? 0 : 4 }}>
                <button
                  type="button"
                  onClick={step.description ? () => setExpandedIndex(isExpanded ? null : globalIndex) : undefined}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    width: '100%',
                    padding: isActive ? '4px 8px' : '0 0 0 8px',
                    cursor: step.description ? 'pointer' : 'default',
                    background: isActive ? 'rgba(var(--color-accent-primary-rgb, 0, 122, 255), 0.08)' : 'none',
                    border: 'none',
                    borderRadius: isActive ? 8 : 0,
                    textAlign: 'left',
                    WebkitTapHighlightColor: 'transparent',
                  }}
                >
                  <Text
                    type={isActive ? 'body' : 'subheadline2'}
                    color={isPending ? 'secondary' : 'primary'}
                    weight={isActive ? 'medium' : 'regular'}
                  >
                    {step.label}
                  </Text>
                  {isCompleted && step.timestamp && (
                    <Text type="caption1" color="secondary" style={{ flexShrink: 0, marginLeft: 8 }}>
                      {formatTimelineDate(step.timestamp)}
                    </Text>
                  )}
                </button>

                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.25, ease: 'easeOut' }}
                      style={{ overflow: 'hidden', paddingLeft: 8 }}
                    >
                      <div style={{ paddingBottom: 4, paddingTop: 2 }}>
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

      {/* Expand collapsed pending steps */}
      {showExpandButton && (
        <button
          type="button"
          onClick={() => setShowAllPending(true)}
          style={{
            display: 'block',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: '6px 0 0 36px',
            fontSize: 13,
            fontWeight: 500,
            color: 'var(--color-accent-primary)',
            WebkitTapHighlightColor: 'transparent',
          }}
        >
          {t('deals.detail.moreSteps', { count: collapsedCount })}
        </button>
      )}
    </div>
  );
}
