import { backButton } from '@telegram-apps/sdk-react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/test/test-utils';
import { BackButtonHandler } from './back-button-handler';

function wrapAvailable<TArgs extends unknown[], TResult>(fn: (...args: TArgs) => TResult) {
  return Object.assign(fn, {
    isAvailable: () => true,
    ifAvailable: (...args: TArgs) => [true, fn(...args)] as const,
  });
}

vi.mock('@telegram-apps/sdk-react', () => {
  const mount = wrapAvailable(vi.fn());
  const show = wrapAvailable(vi.fn());
  const hide = wrapAvailable(vi.fn());
  const onClick = wrapAvailable(vi.fn());
  const offClick = wrapAvailable(vi.fn());

  return {
    backButton: {
      isSupported: vi.fn(() => true),
      isMounted: vi.fn(() => false),
      mount,
      show,
      hide,
      onClick,
      offClick,
    },
  };
});

describe('BackButtonHandler', () => {
  afterEach(() => vi.resetAllMocks());

  it('hides BackButton on root path /catalog', () => {
    renderWithProviders(<BackButtonHandler />, { initialEntries: ['/catalog'] });
    expect(vi.mocked(backButton.hide)).toHaveBeenCalled();
  });

  it('hides BackButton on root path /onboarding', () => {
    renderWithProviders(<BackButtonHandler />, { initialEntries: ['/onboarding'] });
    expect(vi.mocked(backButton.hide)).toHaveBeenCalled();
  });

  it('shows BackButton and registers onClick on non-root path', () => {
    renderWithProviders(<BackButtonHandler />, { initialEntries: ['/deals/123'] });
    expect(vi.mocked(backButton.show)).toHaveBeenCalled();
    expect(vi.mocked(backButton.onClick)).toHaveBeenCalled();
  });

  it('calls offClick on unmount for non-root path', () => {
    const { unmount } = renderWithProviders(<BackButtonHandler />, { initialEntries: ['/deals/123'] });
    unmount();
    expect(vi.mocked(backButton.offClick)).toHaveBeenCalled();
  });

  it('does not throw when SDK methods are unavailable', () => {
    vi.mocked(backButton.isSupported).mockImplementation(() => {
      throw new Error('not supported');
    });
    expect(() => {
      renderWithProviders(<BackButtonHandler />, { initialEntries: ['/deals/123'] });
    }).not.toThrow();
  });
});
