import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { PulsingDot } from '@/shared/ui';
import { CheckCircleIcon } from '@/shared/ui/icons';
import { getTimelineConfig } from '../lib/deal-macro-stage';
import type { DealStatus } from '../types/deal';

type MiniTimelineProps = {
  currentStatus: DealStatus;
};

export function MiniTimeline({ currentStatus }: MiniTimelineProps) {
  const { t } = useTranslation();
  const steps = getTimelineConfig(currentStatus);

  return (
    <div className="flex items-center gap-1">
      {steps.map((step, i) => (
        <div key={step.stage} className="flex items-center gap-1">
          {i > 0 && <Line completed={step.state === 'completed' || step.state === 'active'} />}
          <div className="flex flex-col items-center gap-0.5">
            <Node state={step.state} />
            <Text type="caption2" color="secondary">
              {t(step.i18nKey)}
            </Text>
          </div>
        </div>
      ))}
    </div>
  );
}

function Node({ state }: { state: 'completed' | 'active' | 'pending' }) {
  if (state === 'completed') {
    return (
      <span data-testid="mini-timeline-check" className="flex items-center justify-center">
        <CheckCircleIcon size={14} strokeWidth={2} className="text-accent" />
      </span>
    );
  }

  if (state === 'active') {
    return (
      <span data-testid="mini-timeline-active" className="flex items-center justify-center">
        <PulsingDot color="accent" />
      </span>
    );
  }

  return <span data-testid="mini-timeline-pending" className="block h-2 w-2 shrink-0 rounded-full bg-separator" />;
}

function Line({ completed }: { completed: boolean }) {
  return (
    <span
      data-testid="mini-timeline-line"
      className={`h-px w-4 shrink-0 ${completed ? 'bg-accent' : 'bg-separator'}`}
    />
  );
}
