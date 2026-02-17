import { describe, expect, it } from 'vitest';
import { buttonUrlSchema, findFirstInvalidButtonUrl } from './creative';

describe('creative button URL validation', () => {
  it('accepts http/https URLs', () => {
    expect(buttonUrlSchema.safeParse('https://example.com').success).toBe(true);
    expect(buttonUrlSchema.safeParse('http://example.com/path').success).toBe(true);
  });

  it('rejects non-web URLs', () => {
    expect(buttonUrlSchema.safeParse('example').success).toBe(false);
    expect(buttonUrlSchema.safeParse('ftp://example.com').success).toBe(false);
  });

  it('finds first invalid button URL in keyboard rows', () => {
    const invalid = findFirstInvalidButtonUrl([
      [{ id: 'b1', text: 'ok', url: 'https://example.com' }],
      [{ id: 'b2', text: 'bad', url: 'not-url' }],
      [{ id: 'b3', text: 'next', url: 'https://example.org' }],
    ]);

    expect(invalid).toEqual({ rowIndex: 1, buttonIndex: 0 });
  });
});
