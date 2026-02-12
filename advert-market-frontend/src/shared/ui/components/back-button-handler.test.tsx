import { installTelegramMock, removeTelegramMock } from '@/test/mocks/telegram';
import { renderWithProviders } from '@/test/test-utils';
import { BackButtonHandler } from './back-button-handler';

describe('BackButtonHandler', () => {
  afterEach(() => {
    removeTelegramMock();
  });

  it('hides BackButton on root path /catalog', () => {
    const mock = installTelegramMock();
    renderWithProviders(<BackButtonHandler />, { initialEntries: ['/catalog'] });
    expect(mock.BackButton.hide).toHaveBeenCalled();
  });

  it('hides BackButton on root path /onboarding', () => {
    const mock = installTelegramMock();
    renderWithProviders(<BackButtonHandler />, { initialEntries: ['/onboarding'] });
    expect(mock.BackButton.hide).toHaveBeenCalled();
  });

  it('shows BackButton and registers onClick on non-root path', () => {
    const mock = installTelegramMock();
    renderWithProviders(<BackButtonHandler />, { initialEntries: ['/deals/123'] });
    expect(mock.BackButton.show).toHaveBeenCalled();
    expect(mock.BackButton.onClick).toHaveBeenCalled();
  });

  it('calls offClick on unmount for non-root path', () => {
    const mock = installTelegramMock();
    const { unmount } = renderWithProviders(<BackButtonHandler />, { initialEntries: ['/deals/123'] });
    unmount();
    expect(mock.BackButton.offClick).toHaveBeenCalled();
  });

  it('does not throw when window.Telegram is not available', () => {
    expect(() => {
      renderWithProviders(<BackButtonHandler />, { initialEntries: ['/deals/123'] });
    }).not.toThrow();
  });
});
