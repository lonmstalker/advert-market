import { retrieveRawInitData } from '@telegram-apps/sdk-react';
import type { z } from 'zod/v4';
import type { PaginationParams } from './types';
import { ApiError, problemDetailSchema } from './types';

const BASE_URL = import.meta.env.VITE_MOCK_API === 'true' ? '' : import.meta.env.VITE_API_BASE_URL || '';
const API_PREFIX = '/api/v1';

function getAuthToken(): string | null {
  return sessionStorage.getItem('access_token');
}

function getInitData(): string {
  try {
    return retrieveRawInitData() ?? '';
  } catch {
    return '';
  }
}

async function request<T>(
  method: string,
  path: string,
  options?: {
    body?: unknown;
    schema?: z.ZodType<T>;
    params?: Record<string, string | number | undefined>;
  },
): Promise<T> {
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

  const token = getAuthToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  } else {
    const initData = getInitData();
    if (initData) {
      headers['X-Telegram-Init-Data'] = initData;
    }
  }

  if (options?.body) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url.toString(), {
    method,
    headers,
    body: options?.body ? JSON.stringify(options.body) : undefined,
  });

  if (!response.ok) {
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

  put<T>(path: string, body?: unknown, options?: { schema?: z.ZodType<T> }) {
    return request<T>('PUT', path, { body, schema: options?.schema });
  },

  delete<T>(path: string) {
    return request<T>('DELETE', path);
  },
};
