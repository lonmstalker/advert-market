import '@testing-library/jest-dom/vitest';
import { afterAll, afterEach, beforeAll, beforeEach, vi } from 'vitest';
import { resetMockState } from './mocks/handlers';
import { server } from './mocks/server';

window.scrollTo = vi.fn() as unknown as typeof window.scrollTo;

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
beforeEach(() => {
  try {
    sessionStorage.clear();
  } catch {
    // ignore
  }
  try {
    localStorage.clear?.();
  } catch {
    // ignore
  }
  resetMockState();
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
