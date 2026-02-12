import {
  hapticFeedbackImpactOccurred,
  hapticFeedbackNotificationOccurred,
  hapticFeedbackSelectionChanged,
  isHapticFeedbackSupported,
} from '@telegram-apps/sdk-react';
import { renderHook } from '@testing-library/react';
import { useHaptic } from '@/shared/hooks/use-haptic';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
  hapticFeedbackImpactOccurred: vi.fn(),
  hapticFeedbackNotificationOccurred: vi.fn(),
  hapticFeedbackSelectionChanged: vi.fn(),
  isHapticFeedbackSupported: vi.fn(() => false),
}));

describe('useHaptic', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(isHapticFeedbackSupported).mockReturnValue(false);
  });

  it('impactOccurred is no-op when not supported', () => {
    const { result } = renderHook(() => useHaptic());

    result.current.impactOccurred('medium');

    expect(hapticFeedbackImpactOccurred).not.toHaveBeenCalled();
  });

  it('notificationOccurred is no-op when not supported', () => {
    const { result } = renderHook(() => useHaptic());

    result.current.notificationOccurred('success');

    expect(hapticFeedbackNotificationOccurred).not.toHaveBeenCalled();
  });

  it('selectionChanged is no-op when not supported', () => {
    const { result } = renderHook(() => useHaptic());

    result.current.selectionChanged();

    expect(hapticFeedbackSelectionChanged).not.toHaveBeenCalled();
  });

  it('calls hapticFeedbackImpactOccurred when supported', () => {
    vi.mocked(isHapticFeedbackSupported).mockReturnValue(true);

    const { result } = renderHook(() => useHaptic());

    result.current.impactOccurred('heavy');

    expect(hapticFeedbackImpactOccurred).toHaveBeenCalledWith('heavy');
  });
});
