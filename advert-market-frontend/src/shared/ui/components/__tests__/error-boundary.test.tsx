import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { ErrorBoundary } from '../error-boundary';

function ProblemChild(): JSX.Element {
  throw new Error('Test error');
}

function GoodChild() {
  return <div>All is well</div>;
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    // Suppress React error boundary console.error noise in tests
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  it('renders children when no error occurs', () => {
    renderWithProviders(
      <ErrorBoundary>
        <GoodChild />
      </ErrorBoundary>,
    );
    expect(screen.getByText('All is well')).toBeInTheDocument();
  });

  it('renders error fallback when child throws', () => {
    renderWithProviders(
      <ErrorBoundary>
        <ProblemChild />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    expect(screen.getByText('An unexpected error occurred. Please try again.')).toBeInTheDocument();
  });

  it('renders retry button in error state', () => {
    renderWithProviders(
      <ErrorBoundary>
        <ProblemChild />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Try again')).toBeInTheDocument();
  });

  it('renders warning icon in error state', () => {
    const { container } = renderWithProviders(
      <ErrorBoundary>
        <ProblemChild />
      </ErrorBoundary>,
    );
    const svg = container.querySelector('svg[aria-hidden="true"]');
    expect(svg).toBeInTheDocument();
  });

  it('resets error state when resetKey changes', () => {
    function ConditionalError({ shouldError }: { shouldError: boolean }) {
      if (shouldError) throw new Error('Conditional error');
      return <div>Recovered</div>;
    }

    const { rerender } = renderWithProviders(
      <ErrorBoundary resetKey="key-1">
        <ConditionalError shouldError={true} />
      </ErrorBoundary>,
    );

    expect(screen.getByText('Something went wrong')).toBeInTheDocument();

    // Re-render with a new resetKey and no error
    rerender(
      <ErrorBoundary resetKey="key-2">
        <ConditionalError shouldError={false} />
      </ErrorBoundary>,
    );

    expect(screen.getByText('Recovered')).toBeInTheDocument();
  });

  it('calls window.location.reload when retry button is clicked', async () => {
    const reloadMock = vi.fn();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, reload: reloadMock },
      writable: true,
    });

    const { user } = renderWithProviders(
      <ErrorBoundary>
        <ProblemChild />
      </ErrorBoundary>,
    );

    await user.click(screen.getByText('Try again'));
    expect(reloadMock).toHaveBeenCalledOnce();
  });

  it('logs error details via componentDidCatch', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    renderWithProviders(
      <ErrorBoundary>
        <ProblemChild />
      </ErrorBoundary>,
    );

    // React calls console.error for the error boundary, and our componentDidCatch also logs
    expect(consoleSpy).toHaveBeenCalled();
    const catchCall = consoleSpy.mock.calls.find(
      (args) => typeof args[0] === 'string' && args[0].includes('ErrorBoundary caught:'),
    );
    expect(catchCall).toBeTruthy();
  });

  it('does not reset when resetKey remains the same', () => {
    const { rerender } = renderWithProviders(
      <ErrorBoundary resetKey="same-key">
        <ProblemChild />
      </ErrorBoundary>,
    );

    expect(screen.getByText('Something went wrong')).toBeInTheDocument();

    // Re-render with the same resetKey
    rerender(
      <ErrorBoundary resetKey="same-key">
        <GoodChild />
      </ErrorBoundary>,
    );

    // Still showing error because resetKey did not change
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });
});
