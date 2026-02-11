import { z } from 'zod/v4';

// --- Pagination ---

export const paginatedResponseSchema = <T extends z.ZodType>(itemSchema: T) =>
  z.object({
    items: z.array(itemSchema),
    nextCursor: z.nullable(z.string()),
    hasNext: z.boolean(),
  });

export type PaginatedResponse<T> = {
  items: T[];
  nextCursor: string | null;
  hasNext: boolean;
};

export type PaginationParams = {
  cursor?: string;
  limit?: number;
};

// --- Error (RFC 7807) ---

export const problemDetailSchema = z.object({
  type: z.string(),
  title: z.string(),
  status: z.number(),
  detail: z.string().optional(),
  instance: z.string().optional(),
});

export type ProblemDetail = z.infer<typeof problemDetailSchema>;

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem: ProblemDetail,
  ) {
    super(problem.detail ?? problem.title);
    this.name = 'ApiError';
  }
}

// --- Auth ---

export const authResponseSchema = z.object({
  accessToken: z.string(),
  expiresIn: z.number(),
  user: z.object({
    id: z.number(),
    username: z.string(),
    displayName: z.string(),
  }),
});

export type AuthResponse = z.infer<typeof authResponseSchema>;
