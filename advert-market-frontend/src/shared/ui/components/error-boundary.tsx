import { Button, Text } from '@telegram-tools/ui-kit';
import type { ErrorInfo, ReactNode } from 'react';
import { Component } from 'react';
import type { WithTranslation } from 'react-i18next';
import { withTranslation } from 'react-i18next';

type ErrorBoundaryProps = WithTranslation & {
  children: ReactNode;
  resetKey?: string;
};

type ErrorBoundaryState = {
  hasError: boolean;
  error: Error | null;
};

class ErrorBoundaryInner extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false, error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, info.componentStack);
  }

  componentDidUpdate(prevProps: ErrorBoundaryProps) {
    if (this.state.hasError && prevProps.resetKey !== this.props.resetKey) {
      this.setState({ hasError: false, error: null });
    }
  }

  handleRetry = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      const { t } = this.props;
      return (
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100vh',
            padding: 24,
            textAlign: 'center',
          }}
        >
          <div
            style={{
              width: 80,
              height: 80,
              borderRadius: 20,
              background: 'var(--color-background-base)',
              border: '1px solid var(--color-border-separator)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: 20,
            }}
          >
            <svg
              width="36"
              height="36"
              viewBox="0 0 24 24"
              fill="none"
              aria-hidden="true"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"
                stroke="var(--color-state-warning)"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <line
                x1="12"
                y1="9"
                x2="12"
                y2="13"
                stroke="var(--color-state-warning)"
                strokeWidth="2"
                strokeLinecap="round"
              />
              <circle cx="12" cy="17" r="1" fill="var(--color-state-warning)" />
            </svg>
          </div>

          <div style={{ marginBottom: 8 }}>
            <Text type="title2" weight="bold">
              {t('error.boundary.title')}
            </Text>
          </div>
          <div style={{ marginBottom: 24, maxWidth: 280 }}>
            <Text type="subheadline2" color="secondary">
              {t('error.boundary.description')}
            </Text>
          </div>

          <div style={{ width: '100%', maxWidth: 240 }}>
            <Button text={t('error.boundary.retry')} type="primary" onClick={this.handleRetry} />
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export const ErrorBoundary = withTranslation()(ErrorBoundaryInner);
