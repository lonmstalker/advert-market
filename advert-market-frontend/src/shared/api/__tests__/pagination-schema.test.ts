import { describe, expect, it } from 'vitest';
import { z } from 'zod/v4';
import { paginatedResponseSchema } from '../types';

const itemSchema = z.object({ id: z.number() });

describe('paginatedResponseSchema', () => {
  it('parses response with nextCursor present', () => {
    const result = paginatedResponseSchema(itemSchema).parse({
      items: [{ id: 1 }],
      nextCursor: 'abc123',
      hasNext: true,
    });

    expect(result.items).toEqual([{ id: 1 }]);
    expect(result.nextCursor).toBe('abc123');
    expect(result.hasNext).toBe(true);
  });

  it('parses response with nextCursor: null', () => {
    const result = paginatedResponseSchema(itemSchema).parse({
      items: [],
      nextCursor: null,
      hasNext: false,
    });

    expect(result.nextCursor).toBeNull();
    expect(result.hasNext).toBe(false);
  });

  it('parses response when nextCursor is absent (Jackson non_null)', () => {
    const result = paginatedResponseSchema(itemSchema).parse({
      items: [],
      hasNext: false,
    });

    expect(result.nextCursor).toBeNull();
    expect(result.hasNext).toBe(false);
    expect(result.items).toEqual([]);
  });
});
