import { copyToClipboard } from '@/shared/lib/clipboard';

describe('copyToClipboard', () => {
  const originalExecCommand = document.execCommand;

  beforeEach(() => {
    // Ensure execCommand exists on document for jsdom
    if (typeof document.execCommand !== 'function') {
      document.execCommand = vi.fn();
    }
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.execCommand = originalExecCommand;
  });

  it('returns true when navigator.clipboard.writeText succeeds', async () => {
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: writeTextMock },
      writable: true,
      configurable: true,
    });

    const result = await copyToClipboard('hello');

    expect(result).toBe(true);
    expect(writeTextMock).toHaveBeenCalledWith('hello');
  });

  it('falls back to execCommand when writeText throws and returns true', async () => {
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: vi.fn().mockRejectedValue(new Error('denied')) },
      writable: true,
      configurable: true,
    });

    const execCommandMock = vi.spyOn(document, 'execCommand').mockReturnValue(true);

    const result = await copyToClipboard('fallback text');

    expect(result).toBe(true);
    expect(execCommandMock).toHaveBeenCalledWith('copy');
  });

  it('returns false when both clipboard API and execCommand fail', async () => {
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: vi.fn().mockRejectedValue(new Error('denied')) },
      writable: true,
      configurable: true,
    });

    vi.spyOn(document, 'execCommand').mockImplementation(() => {
      throw new Error('execCommand failed');
    });

    const result = await copyToClipboard('fail text');

    expect(result).toBe(false);
  });
});
