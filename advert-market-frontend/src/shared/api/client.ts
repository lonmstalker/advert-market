import type { z } from 'zod/v4';
import { getTelegramInitData } from '@/shared/lib/telegram-init-data';
import type { PaginationParams } from './types';
import { ApiError, problemDetailSchema } from './types';

const BASE_URL = import.meta.env.VITE_MOCK_API === 'true' ? '' : import.meta.env.VITE_API_BASE_URL || '';
const API_PREFIX = '/api/v1';
const REQUEST_TIMEOUT_MS =
  Number(import.meta.env.VITE_API_TIMEOUT_MS) > 0 ? Number(import.meta.env.VITE_API_TIMEOUT_MS) : 15_000;

function getAuthToken(): string | null {
  return sessionStorage.getItem('access_token');
}

function isAbortError(err: unknown): boolean {
  return typeof err === 'object' && err !== null && 'name' in err && (err as { name?: unknown }).name === 'AbortError';
}

function deriveCanaryKey(initData: string): string {
  if (!initData) return '';
  try {
    const params = new URLSearchParams(initData);

    // Prefer stable Telegram user.id when available (best for per-user canary routing).
    const userJson = params.get('user');
    if (userJson) {
      try {
        const user = JSON.parse(userJson) as { id?: unknown };
        const id = user?.id;
        if (typeof id === 'number' || typeof id === 'string') {
          return String(id);
        }
      } catch {
        // Ignore invalid user JSON.
      }
    }

    // Fallback: chat_instance is stable within Telegram context.
    return params.get('chat_instance') ?? '';
  } catch {
    return '';
  }
}

type RequestOptions<T> = {
  body?: unknown;
  schema?: z.ZodType<T>;
  params?: Record<string, string | number | undefined>;
  _isRetrying?: boolean;
};

let reLoginPromise: Promise<boolean> | null = null;

async function attemptReLogin(): Promise<boolean> {
  if (reLoginPromise) return reLoginPromise;

  reLoginPromise = doReLogin().finally(() => {
    reLoginPromise = null;
  });
  return reLoginPromise;
}

async function doReLogin(): Promise<boolean> {
  const initData = getTelegramInitData();
  if (!initData) return false;

  try {
    const { login } = await import('./auth');
    await login(initData);
    return true;
  } catch {
    return false;
  }
}

async function request<T>(method: string, path: string, options?: RequestOptions<T>): Promise<T> {
  const url = new URL(`${API_PREFIX}${path}`, BASE_URL || window.location.origin);

  if (options?.params) {
    for (const [key, value] of Object.entries(options.params)) {
      if (value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    }
  }

  const headers: Record<string, string> = {
    Accept: 'application/json',
  };

  const initData = getTelegramInitData();

  const token = getAuthToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  } else {
    if (initData) {
      headers['X-Telegram-Init-Data'] = initData;
    }
  }

  if (options?.body) {
    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }
  }

  const canaryKey = deriveCanaryKey(initData);
  if (canaryKey) {
    headers['X-Canary-Key'] = canaryKey;
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  let response: Response;
  try {
    response = await fetch(url.toString(), {
      method,
      headers,
      body: options?.body
        ? options.body instanceof FormData
          ? options.body
          : JSON.stringify(options.body)
        : undefined,
      signal: controller.signal,
    });
  } catch (err) {
    if (isAbortError(err)) {
      throw new ApiError(0, {
        type: 'about:blank',
        title: 'Request timed out',
        status: 0,
        detail: `Request to ${url.pathname} timed out after ${REQUEST_TIMEOUT_MS}ms`,
      });
    }
    throw err;
  } finally {
    clearTimeout(timeoutId);
  }

  if (!response.ok) {
    // Never attempt auto re-login for auth endpoints themselves.
    // Otherwise we can deadlock by trying to "re-login" while already logging in.
    const isAuthEndpoint = path.startsWith('/auth/');
    if (response.status === 401 && !isAuthEndpoint && !options?._isRetrying) {
      sessionStorage.removeItem('access_token');
      const reLoginOk = await attemptReLogin();
      if (reLoginOk) {
        return request(method, path, { ...options, _isRetrying: true });
      }
    }

    const errorBody: unknown = await response.json().catch(() => null);
    const parsed = problemDetailSchema.safeParse(errorBody);
    throw new ApiError(
      response.status,
      parsed.success ? parsed.data : { type: 'about:blank', title: response.statusText, status: response.status },
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const data: unknown = await response.json();

  if (options?.schema) {
    return options.schema.parse(data);
  }

  return data as T;
}

export const api = {
  get<T>(
    path: string,
    options?: { schema?: z.ZodType<T>; params?: PaginationParams & Record<string, string | number | undefined> },
  ) {
    return request<T>('GET', path, options);
  },

  post<T>(path: string, body?: unknown, options?: { schema?: z.ZodType<T> }) {
    return request<T>('POST', path, { body, schema: options?.schema });
  },

  postForm<T>(path: string, formData: FormData, options?: { schema?: z.ZodType<T> }) {
    return request<T>('POST', path, { body: formData, schema: options?.schema });
  },

  put<T>(path: string, body?: unknown, options?: { schema?: z.ZodType<T> }) {
    return request<T>('PUT', path, { body, schema: options?.schema });
  },

  delete(path: string) {
    return request<void>('DELETE', path);
  },
};
