import { Button, Sheet, Text } from '@telegram-tools/ui-kit';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Chip } from '@/shared/ui';

type DealFilterSheetProps = {
  open: boolean;
  onClose: () => void;
  activeStatuses: Set<string>;
  onApply: (statuses: Set<string>) => void;
  onReset: () => void;
};

const STATUS_GROUPS = [
  {
    i18nKey: 'deals.filter.new',
    statuses: ['DRAFT', 'OFFER_PENDING', 'NEGOTIATING'],
  },
  {
    i18nKey: 'deals.filter.active',
    statuses: ['ACCEPTED', 'AWAITING_PAYMENT', 'FUNDED', 'CREATIVE_SUBMITTED', 'CREATIVE_APPROVED'],
  },
  {
    i18nKey: 'deals.filter.publishing',
    statuses: ['SCHEDULED', 'PUBLISHED', 'DELIVERY_VERIFYING'],
  },
  {
    i18nKey: 'deals.filter.completed',
    statuses: ['COMPLETED_RELEASED'],
  },
  {
    i18nKey: 'deals.filter.cancelled',
    statuses: ['CANCELLED', 'EXPIRED', 'REFUNDED'],
  },
  {
    i18nKey: 'deals.filter.disputed',
    statuses: ['DISPUTED'],
  },
];

export function DealFilterSheet({ open, onClose, activeStatuses, onApply, onReset }: DealFilterSheetProps) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<Set<string>>(new Set(activeStatuses));

  const toggleGroup = (statuses: string[]) => {
    setDraft((prev) => {
      const next = new Set(prev);
      const allActive = statuses.every((s) => next.has(s));
      if (allActive) {
        for (const s of statuses) next.delete(s);
      } else {
        for (const s of statuses) next.add(s);
      }
      return next;
    });
  };

  const handleApply = () => {
    onApply(draft);
    onClose();
  };

  const handleReset = () => {
    setDraft(new Set());
    onReset();
  };

  const FiltersSheet = () => (
    <div className="flex flex-col gap-5 px-4 py-5">
      <Text type="title2" weight="bold">
        {t('deals.filter.title')}
      </Text>

      <div className="flex flex-wrap gap-2.5">
        {STATUS_GROUPS.map((group) => {
          const allActive = group.statuses.every((s) => draft.has(s));
          return (
            <Chip
              key={group.i18nKey}
              variant="rounded"
              label={t(group.i18nKey)}
              active={allActive}
              onClick={() => toggleGroup(group.statuses)}
            />
          );
        })}
      </div>

      <div className="flex gap-2.5">
        <div className="flex-1">
          <Button text={t('common.reset')} type="secondary" onClick={handleReset} />
        </div>
        <div className="flex-1">
          <Button text={t('common.apply')} type="primary" onClick={handleApply} />
        </div>
      </div>
    </div>
  );

  return <Sheet sheets={{ filters: FiltersSheet }} activeSheet="filters" opened={open} onClose={onClose} />;
}
