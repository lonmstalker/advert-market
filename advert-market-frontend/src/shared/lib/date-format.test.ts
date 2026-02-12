import { formatDate, formatDateTime, formatRelativeTime } from '@/shared/lib/date-format';

describe('date-format', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  describe('formatRelativeTime', () => {
    it('returns "just now" for less than 60 seconds ago', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2025-06-15T12:00:30Z'));

      expect(formatRelativeTime(new Date('2025-06-15T12:00:00Z'))).toBe('just now');
    });

    it('returns minutes for 5 minutes ago', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2025-06-15T12:05:00Z'));

      const result = formatRelativeTime(new Date('2025-06-15T12:00:00Z'));
      expect(result).toContain('5');
      expect(result).toContain('minute');
    });

    it('returns hours for 3 hours ago', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2025-06-15T15:00:00Z'));

      const result = formatRelativeTime(new Date('2025-06-15T12:00:00Z'));
      expect(result).toContain('3');
      expect(result).toContain('hour');
    });

    it('returns days for 2 days ago', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2025-06-17T12:00:00Z'));

      const result = formatRelativeTime(new Date('2025-06-15T12:00:00Z'));
      expect(result).toContain('2');
      expect(result).toContain('day');
    });

    it('falls back to formatDate for more than 7 days ago', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2025-06-25T12:00:00Z'));

      const target = new Date('2025-06-15T12:00:00Z');
      const result = formatRelativeTime(target);
      const formatted = formatDate(target);
      expect(result).toBe(formatted);
    });

    it('accepts ISO string input', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2025-06-15T12:00:30Z'));

      expect(formatRelativeTime('2025-06-15T12:00:00Z')).toBe('just now');
    });
  });

  describe('formatDate', () => {
    it('returns a string containing month and day', () => {
      const result = formatDate(new Date('2025-06-15T12:00:00Z'));
      expect(result).toContain('Jun');
      expect(result).toContain('15');
    });

    it('returns a string containing the year', () => {
      const result = formatDate(new Date('2025-06-15T12:00:00Z'));
      expect(result).toContain('2025');
    });

    it('accepts ISO string input', () => {
      const result = formatDate('2025-06-15T12:00:00Z');
      expect(typeof result).toBe('string');
      expect(result).toContain('15');
    });
  });

  describe('formatDateTime', () => {
    it('returns a string containing date and time parts', () => {
      const result = formatDateTime(new Date('2025-06-15T14:30:00Z'));
      expect(result).toContain('Jun');
      expect(result).toContain('15');
      expect(result).toContain('2025');
      // Time part should be present (hours and minutes)
      expect(result).toMatch(/\d{1,2}:\d{2}/);
    });

    it('accepts ISO string input', () => {
      const result = formatDateTime('2025-06-15T14:30:00Z');
      expect(typeof result).toBe('string');
      expect(result).toMatch(/\d{1,2}:\d{2}/);
    });
  });
});
