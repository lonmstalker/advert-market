import { useEffect, useState } from 'react';

type TFunction = (key: string, opts?: Record<string, unknown>) => string;

/**
 * Returns a localized countdown string that updates every minute.
 * Returns null if no deadline or if the deadline has passed.
 */
export function useCountdown(deadlineAt: string | null, t: TFunction): string | null {
  const [remaining, setRemaining] = useState<string | null>(null);

  useEffect(() => {
    if (!deadlineAt) {
      setRemaining(null);
      return;
    }

    function update() {
      const diff = new Date(deadlineAt as string).getTime() - Date.now();
      if (diff <= 0) {
        setRemaining(null);
        return;
      }
      const totalHours = Math.floor(diff / 3_600_000);
      const minutes = Math.floor((diff % 3_600_000) / 60_000);
      if (totalHours >= 24) {
        const days = Math.floor(totalHours / 24);
        const hours = totalHours % 24;
        setRemaining(t('deals.detail.deadlineDays', { days, hours }));
      } else if (totalHours > 0) {
        setRemaining(t('deals.detail.deadlineHours', { hours: totalHours, minutes }));
      } else {
        setRemaining(t('deals.detail.deadlineMinutes', { minutes }));
      }
    }

    update();
    const interval = setInterval(update, 60_000);
    return () => clearInterval(interval);
  }, [deadlineAt, t]);

  return remaining;
}
