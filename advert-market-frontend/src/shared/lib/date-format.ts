function toDate(date: string | Date): Date {
  return typeof date === 'string' ? new Date(date) : date;
}

export function formatDate(date: string | Date, locale = 'en'): string {
  return new Intl.DateTimeFormat(locale, { year: 'numeric', month: 'short', day: 'numeric' }).format(toDate(date));
}

export function formatDateTime(date: string | Date, locale = 'en'): string {
  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(toDate(date));
}

export function formatRelativeTime(date: string | Date, locale = 'en'): string {
  const target = toDate(date);
  const now = Date.now();
  const diffMs = now - target.getTime();
  const diffSec = Math.floor(diffMs / 1000);

  if (diffSec < 60) {
    const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });
    return rtf.format(0, 'second');
  }

  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });

  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return rtf.format(-diffMin, 'minute');

  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return rtf.format(-diffHours, 'hour');

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return rtf.format(-diffDays, 'day');

  return formatDate(date, locale);
}
