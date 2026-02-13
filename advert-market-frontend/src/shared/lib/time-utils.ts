type TFunction = (key: string, opts?: Record<string, unknown>) => string;

/**
 * Formats the time remaining until a given ISO date string.
 * Returns null if the date is in the past.
 */
export function formatTimeUntil(isoDate: string): string | null {
  const target = new Date(isoDate);
  const now = new Date();
  const diffMs = target.getTime() - now.getTime();
  if (diffMs <= 0) return null;
  const hours = Math.floor(diffMs / (1000 * 60 * 60));
  const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
  if (hours > 24) {
    const days = Math.floor(hours / 24);
    return `${days}d ${hours % 24}h`;
  }
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

/**
 * Formats the age of a channel relative to now using i18n keys.
 */
export function formatChannelAge(createdAt: string, t: TFunction): string {
  const created = new Date(createdAt);
  const now = new Date();
  const diffMs = now.getTime() - created.getTime();
  const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (days < 1) return t('catalog.channel.addedToday');
  if (days < 30) return t('catalog.channel.addedDaysAgo', { count: days });
  const months = Math.floor(days / 30);
  if (months < 12) return t('catalog.channel.addedMonthsAgo', { count: months });
  const monthNames = [
    'января',
    'февраля',
    'марта',
    'апреля',
    'мая',
    'июня',
    'июля',
    'августа',
    'сентября',
    'октября',
    'ноября',
    'декабря',
  ];
  return t('catalog.channel.onPlatformSince', {
    date: `${monthNames[created.getMonth()]} ${created.getFullYear()}`,
  });
}
