import '@testing-library/jest-dom/vitest';
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest';
import { resetMockState } from './mocks/handlers';
import { server } from './mocks/server';

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
