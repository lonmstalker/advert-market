import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useInfiniteScroll } from '../use-infinite-scroll';

type ObserverCallback = (entries: IntersectionObserverEntry[]) => void;

let observeMock: ReturnType<typeof vi.fn>;
let disconnectMock: ReturnType<typeof vi.fn>;
let capturedCallback: ObserverCallback;
let capturedOptions: IntersectionObserverInit | undefined;

/**
 * Test component that wires the sentinel ref to a real DOM element,
 * ensuring the IntersectionObserver setup runs in the useEffect.
 */
function TestComponent(props: {
  hasNextPage: boolean | undefined;
  isFetchingNextPage: boolean;
  fetchNextPage: () => void;
  rootMargin?: string;
  threshold?: number;
}) {
  const sentinelRef = useInfiniteScroll(props);
  return <div ref={sentinelRef} data-testid="sentinel" />;
}

describe('useInfiniteScroll', () => {
  beforeEach(() => {
    observeMock = vi.fn();
    disconnectMock = vi.fn();

    vi.stubGlobal(
      'IntersectionObserver',
      vi.fn(function MockIO(this: unknown, callback: ObserverCallback, options?: IntersectionObserverInit) {
        capturedCallback = callback;
        capturedOptions = options;
        return { observe: observeMock, disconnect: disconnectMock, unobserve: vi.fn() };
      }),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns a ref that can be attached to a DOM element', () => {
    render(<TestComponent hasNextPage={true} isFetchingNextPage={false} fetchNextPage={vi.fn()} />);
    expect(screen.getByTestId('sentinel')).toBeInTheDocument();
  });

  it('creates IntersectionObserver and observes the sentinel element', () => {
    render(<TestComponent hasNextPage={true} isFetchingNextPage={false} fetchNextPage={vi.fn()} />);
    expect(IntersectionObserver).toHaveBeenCalledOnce();
    expect(observeMock).toHaveBeenCalledOnce();
    expect(observeMock).toHaveBeenCalledWith(screen.getByTestId('sentinel'));
  });

  it('uses default rootMargin of 200px and threshold of 0', () => {
    render(<TestComponent hasNextPage={true} isFetchingNextPage={false} fetchNextPage={vi.fn()} />);
    expect(capturedOptions).toEqual({ rootMargin: '200px', threshold: 0 });
  });

  it('passes custom rootMargin and threshold to IntersectionObserver', () => {
    render(
      <TestComponent
        hasNextPage={true}
        isFetchingNextPage={false}
        fetchNextPage={vi.fn()}
        rootMargin="300px"
        threshold={0.5}
      />,
    );
    expect(capturedOptions).toEqual({ rootMargin: '300px', threshold: 0.5 });
  });

  it('calls fetchNextPage when intersection occurs and hasNextPage is true', () => {
    const fetchNextPage = vi.fn();
    render(<TestComponent hasNextPage={true} isFetchingNextPage={false} fetchNextPage={fetchNextPage} />);

    capturedCallback([{ isIntersecting: true } as IntersectionObserverEntry]);
    expect(fetchNextPage).toHaveBeenCalledOnce();
  });

  it('does not call fetchNextPage when hasNextPage is false', () => {
    const fetchNextPage = vi.fn();
    render(<TestComponent hasNextPage={false} isFetchingNextPage={false} fetchNextPage={fetchNextPage} />);

    capturedCallback([{ isIntersecting: true } as IntersectionObserverEntry]);
    expect(fetchNextPage).not.toHaveBeenCalled();
  });

  it('does not call fetchNextPage when hasNextPage is undefined', () => {
    const fetchNextPage = vi.fn();
    render(<TestComponent hasNextPage={undefined} isFetchingNextPage={false} fetchNextPage={fetchNextPage} />);

    capturedCallback([{ isIntersecting: true } as IntersectionObserverEntry]);
    expect(fetchNextPage).not.toHaveBeenCalled();
  });

  it('does not call fetchNextPage when already fetching', () => {
    const fetchNextPage = vi.fn();
    render(<TestComponent hasNextPage={true} isFetchingNextPage={true} fetchNextPage={fetchNextPage} />);

    capturedCallback([{ isIntersecting: true } as IntersectionObserverEntry]);
    expect(fetchNextPage).not.toHaveBeenCalled();
  });

  it('does not call fetchNextPage when sentinel is not intersecting', () => {
    const fetchNextPage = vi.fn();
    render(<TestComponent hasNextPage={true} isFetchingNextPage={false} fetchNextPage={fetchNextPage} />);

    capturedCallback([{ isIntersecting: false } as IntersectionObserverEntry]);
    expect(fetchNextPage).not.toHaveBeenCalled();
  });

  it('handles empty entries array without error', () => {
    const fetchNextPage = vi.fn();
    render(<TestComponent hasNextPage={true} isFetchingNextPage={false} fetchNextPage={fetchNextPage} />);

    expect(() => capturedCallback([])).not.toThrow();
    expect(fetchNextPage).not.toHaveBeenCalled();
  });

  it('disconnects observer on unmount', () => {
    const { unmount } = render(<TestComponent hasNextPage={true} isFetchingNextPage={false} fetchNextPage={vi.fn()} />);

    unmount();
    expect(disconnectMock).toHaveBeenCalled();
  });
});
