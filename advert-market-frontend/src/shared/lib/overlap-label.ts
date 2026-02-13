type TFunction = (key: string, opts?: Record<string, unknown>) => string;

export type OverlapResult = {
  label: string;
  hasTooltip: boolean;
  freq?: number;
  dur?: number;
};

/**
 * Builds an overlap label string from frequency/duration, used in deals and pricing rules.
 * Returns null if neither freq nor dur is provided.
 */
export function buildOverlapLabel(
  freq: number | undefined | null,
  dur: number | undefined | null,
  t: TFunction,
): OverlapResult | null {
  if (freq && dur) {
    return {
      label: t('catalog.channel.overlapFormat', { freq, dur }),
      hasTooltip: true,
      freq,
      dur,
    };
  }
  if (dur) {
    return { label: t('catalog.channel.onlyDuration', { dur }), hasTooltip: false };
  }
  if (freq) {
    return { label: t('catalog.channel.onlyFrequency', { freq }), hasTooltip: false };
  }
  return null;
}
