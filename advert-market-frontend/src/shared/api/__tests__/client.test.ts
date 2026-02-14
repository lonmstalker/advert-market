import { HttpResponse, http } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { z } from 'zod/v4';
import { server } from '@/test/mocks/server';
import { ApiError } from '../types';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

// Dynamic import so the mock is applied before the module loads
const { api } = await import('../client');
const { retrieveRawInitData } = await import('@telegram-apps/sdk-react');

const API_BASE = '/api/v1';

describe('api client', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  // -------- Successful requests --------

  describe('successful requests', () => {
    it('GET returns parsed JSON body', async () => {
      server.use(
        http.get(`${API_BASE}/test`, () => {
          return HttpResponse.json({ message: 'ok' });
        }),
      );

      const data = await api.get<{ message: string }>('/test');
      expect(data).toEqual({ message: 'ok' });
    });

    it('GET validates response with Zod schema', async () => {
      const schema = z.object({ id: z.number(), name: z.string() });
      server.use(
        http.get(`${API_BASE}/items/1`, () => {
          return HttpResponse.json({ id: 1, name: 'Widget' });
        }),
      );

      const item = await api.get('/items/1', { schema });
      expect(item).toEqual({ id: 1, name: 'Widget' });
    });

    it('GET appends query params and omits undefined values', async () => {
      server.use(
        http.get(`${API_BASE}/search`, ({ request }) => {
          const url = new URL(request.url);
          return HttpResponse.json({
            q: url.searchParams.get('q'),
            limit: url.searchParams.get('limit'),
            cursor: url.searchParams.get('cursor'),
          });
        }),
      );

      const data = await api.get<{ q: string; limit: string; cursor: string | null }>('/search', {
        params: { q: 'test', limit: 10, cursor: undefined },
      });
      expect(data.q).toBe('test');
      expect(data.limit).toBe('10');
      expect(data.cursor).toBeNull();
    });

    it('POST sends JSON body and returns response', async () => {
      server.use(
        http.post(`${API_BASE}/items`, async ({ request }) => {
          const body = await request.json();
          return HttpResponse.json({ ...body, id: 42 }, { status: 201 });
        }),
      );

      const result = await api.post<{ id: number; name: string }>('/items', { name: 'New Item' });
      expect(result).toEqual({ id: 42, name: 'New Item' });
    });

    it('PUT sends JSON body and returns response', async () => {
      server.use(
        http.put(`${API_BASE}/items/1`, async ({ request }) => {
          const body = await request.json();
          return HttpResponse.json(body);
        }),
      );

      const result = await api.put<{ name: string }>('/items/1', { name: 'Updated' });
      expect(result).toEqual({ name: 'Updated' });
    });

    it('DELETE returns undefined for 204 response', async () => {
      server.use(
        http.delete(`${API_BASE}/items/1`, () => {
          return new HttpResponse(null, { status: 204 });
        }),
      );

      const result = await api.delete('/items/1');
      expect(result).toBeUndefined();
    });
  });

  // -------- Auth headers --------

  describe('authorization headers', () => {
    it('sends Bearer token when access_token is in sessionStorage', async () => {
      sessionStorage.setItem('access_token', 'jwt-token-123');

      server.use(
        http.get(`${API_BASE}/secure`, ({ request }) => {
          const auth = request.headers.get('Authorization');
          return HttpResponse.json({ auth });
        }),
      );

      const data = await api.get<{ auth: string }>('/secure');
      expect(data.auth).toBe('Bearer jwt-token-123');
    });

    it('sends X-Telegram-Init-Data when no access_token exists', async () => {
      server.use(
        http.get(`${API_BASE}/init`, ({ request }) => {
          return HttpResponse.json({
            initData: request.headers.get('X-Telegram-Init-Data'),
            auth: request.headers.get('Authorization'),
          });
        }),
      );

      const data = await api.get<{ initData: string; auth: string | null }>('/init');
      expect(data.initData).toBe('mock-init-data');
      expect(data.auth).toBeNull();
    });

    it('prefers Bearer token over initData when both available', async () => {
      sessionStorage.setItem('access_token', 'my-jwt');

      server.use(
        http.get(`${API_BASE}/pref`, ({ request }) => {
          return HttpResponse.json({
            auth: request.headers.get('Authorization'),
            initData: request.headers.get('X-Telegram-Init-Data'),
          });
        }),
      );

      const data = await api.get<{ auth: string; initData: string | null }>('/pref');
      expect(data.auth).toBe('Bearer my-jwt');
      expect(data.initData).toBeNull();
    });
  });

  // -------- Error parsing --------

  describe('error parsing', () => {
    it('throws ApiError with ProblemDetail for RFC 7807 error response', async () => {
      server.use(
        http.get(`${API_BASE}/fail`, () => {
          return HttpResponse.json(
            {
              type: 'urn:problem:validation',
              title: 'Validation Error',
              status: 422,
              detail: 'Field "name" is required',
            },
            { status: 422 },
          );
        }),
      );

      try {
        await api.get('/fail');
        expect.unreachable('should have thrown');
      } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        const apiError = err as ApiError;
        expect(apiError.status).toBe(422);
        expect(apiError.problem.type).toBe('urn:problem:validation');
        expect(apiError.problem.detail).toBe('Field "name" is required');
        expect(apiError.message).toBe('Field "name" is required');
      }
    });

    it('throws ApiError with fallback ProblemDetail when body is not RFC 7807', async () => {
      server.use(
        http.get(`${API_BASE}/bad`, () => {
          return HttpResponse.json({ error: 'something went wrong' }, { status: 500 });
        }),
      );

      try {
        await api.get('/bad');
        expect.unreachable('should have thrown');
      } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        const apiError = err as ApiError;
        expect(apiError.status).toBe(500);
        expect(apiError.problem.type).toBe('about:blank');
        expect(apiError.problem.title).toBe('Internal Server Error');
      }
    });

    it('throws ApiError with fallback when response body is not JSON', async () => {
      server.use(
        http.get(`${API_BASE}/html-error`, () => {
          return new HttpResponse('<html>Error</html>', {
            status: 502,
            headers: { 'Content-Type': 'text/html' },
          });
        }),
      );

      try {
        await api.get('/html-error');
        expect.unreachable('should have thrown');
      } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        const apiError = err as ApiError;
        expect(apiError.status).toBe(502);
        expect(apiError.problem.type).toBe('about:blank');
      }
    });

    it('Zod schema validation rejects mismatched data', async () => {
      const schema = z.object({ id: z.number(), name: z.string() });
      server.use(
        http.get(`${API_BASE}/mismatch`, () => {
          return HttpResponse.json({ id: 'not-a-number', name: 123 });
        }),
      );

      await expect(api.get('/mismatch', { schema })).rejects.toThrow();
    });
  });

  // -------- Timeouts / Abort --------

  describe('timeouts', () => {
    it('aborts a hanging request after timeout', async () => {
      vi.useFakeTimers();

      const originalFetch = globalThis.fetch;
      try {
        globalThis.fetch = vi.fn((_url: string, init?: RequestInit) => {
          const signal = init?.signal;
          return new Promise((_resolve, reject) => {
            if (signal) {
              signal.addEventListener('abort', () => {
                reject(new DOMException('Aborted', 'AbortError'));
              });
            }
          }) as Promise<Response>;
        }) as typeof fetch;

        const requestPromise = api.get('/hang');

        const outcomePromise = Promise.race([
          requestPromise.then(
            () => 'resolved' as const,
            () => 'rejected' as const,
          ),
          new Promise<'timeout'>((resolve) => setTimeout(() => resolve('timeout'), 20_000)),
        ]);

        await vi.advanceTimersByTimeAsync(20_000);

        const outcome = await outcomePromise;
        expect(outcome).toBe('rejected');
        await expect(requestPromise).rejects.toBeInstanceOf(ApiError);
      } finally {
        globalThis.fetch = originalFetch;
        vi.useRealTimers();
      }
    });

    it('wraps AbortError into ApiError', async () => {
      const originalFetch = globalThis.fetch;
      try {
        globalThis.fetch = vi.fn(() =>
          Promise.reject(new DOMException('Aborted', 'AbortError')),
        ) as unknown as typeof fetch;

        await expect(api.get('/abort')).rejects.toBeInstanceOf(ApiError);
      } finally {
        globalThis.fetch = originalFetch;
      }
    });
  });

  // -------- 401 handling & re-login --------

  describe('401 handling', () => {
    it('clears access_token from sessionStorage on 401', async () => {
      sessionStorage.setItem('access_token', 'expired-token');

      let callCount = 0;
      server.use(
        http.get(`${API_BASE}/protected`, () => {
          callCount++;
          if (callCount === 1) {
            return HttpResponse.json({ type: 'about:blank', title: 'Unauthorized', status: 401 }, { status: 401 });
          }
          return HttpResponse.json({ data: 'ok' });
        }),
        http.post(`${API_BASE}/auth/login`, () => {
          sessionStorage.setItem('access_token', 'new-token');
          return HttpResponse.json({
            accessToken: 'new-token',
            expiresIn: 86400,
            user: { id: 1, username: 'test', displayName: 'Test' },
          });
        }),
      );

      const result = await api.get<{ data: string }>('/protected');
      expect(result.data).toBe('ok');
      expect(callCount).toBe(2);
    });

    it('throws ApiError when re-login fails and request is not retried', async () => {
      sessionStorage.setItem('access_token', 'expired-token');

      server.use(
        http.get(`${API_BASE}/locked`, () => {
          return HttpResponse.json({ type: 'about:blank', title: 'Unauthorized', status: 401 }, { status: 401 });
        }),
        http.post(`${API_BASE}/auth/login`, () => {
          return HttpResponse.json({ type: 'about:blank', title: 'Forbidden', status: 403 }, { status: 403 });
        }),
      );

      try {
        await api.get('/locked');
        expect.unreachable('should have thrown');
      } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect((err as ApiError).status).toBe(401);
      }
    });

    it('does not retry on second consecutive 401 (prevents infinite loop)', async () => {
      sessionStorage.setItem('access_token', 'expired-token');

      let callCount = 0;
      server.use(
        http.get(`${API_BASE}/always-401`, () => {
          callCount++;
          return HttpResponse.json({ type: 'about:blank', title: 'Unauthorized', status: 401 }, { status: 401 });
        }),
        http.post(`${API_BASE}/auth/login`, () => {
          sessionStorage.setItem('access_token', 'still-bad');
          return HttpResponse.json({
            accessToken: 'still-bad',
            expiresIn: 86400,
            user: { id: 1, username: 'test', displayName: 'Test' },
          });
        }),
      );

      await expect(api.get('/always-401')).rejects.toBeInstanceOf(ApiError);
      // First request + one retry = 2 calls total (no infinite loop)
      expect(callCount).toBe(2);
    });
  });

  // -------- Content-Type header --------

  describe('request headers', () => {
    it('sends X-Canary-Key derived from Telegram user.id when available', async () => {
      vi.mocked(retrieveRawInitData).mockReturnValueOnce('user=%7B%22id%22%3A123%7D&hash=abc');

      server.use(
        http.get(`${API_BASE}/canary-key`, ({ request }) => {
          return HttpResponse.json({
            canaryKey: request.headers.get('X-Canary-Key'),
          });
        }),
      );

      const data = await api.get<{ canaryKey: string | null }>('/canary-key');
      expect(data.canaryKey).toBe('123');
    });

    it('still sends X-Canary-Key when Authorization is present', async () => {
      sessionStorage.setItem('access_token', 'my-jwt');
      vi.mocked(retrieveRawInitData).mockReturnValueOnce('user=%7B%22id%22%3A777%7D&hash=abc');

      server.use(
        http.get(`${API_BASE}/canary-key-auth`, ({ request }) => {
          return HttpResponse.json({
            auth: request.headers.get('Authorization'),
            canaryKey: request.headers.get('X-Canary-Key'),
          });
        }),
      );

      const data = await api.get<{ auth: string | null; canaryKey: string | null }>('/canary-key-auth');
      expect(data.auth).toBe('Bearer my-jwt');
      expect(data.canaryKey).toBe('777');
    });

    it('sets Content-Type to application/json when body is provided', async () => {
      server.use(
        http.post(`${API_BASE}/with-body`, ({ request }) => {
          return HttpResponse.json({
            contentType: request.headers.get('Content-Type'),
          });
        }),
      );

      const data = await api.post<{ contentType: string }>('/with-body', { key: 'value' });
      expect(data.contentType).toBe('application/json');
    });

    it('does not set Content-Type when no body is provided', async () => {
      server.use(
        http.get(`${API_BASE}/no-body`, ({ request }) => {
          return HttpResponse.json({
            contentType: request.headers.get('Content-Type'),
          });
        }),
      );

      const data = await api.get<{ contentType: string | null }>('/no-body');
      expect(data.contentType).toBeNull();
    });

    it('always sets Accept to application/json', async () => {
      server.use(
        http.get(`${API_BASE}/accept-check`, ({ request }) => {
          return HttpResponse.json({
            accept: request.headers.get('Accept'),
          });
        }),
      );

      const data = await api.get<{ accept: string }>('/accept-check');
      expect(data.accept).toBe('application/json');
    });
  });
});
