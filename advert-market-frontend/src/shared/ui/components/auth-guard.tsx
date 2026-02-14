import { Button, Spinner, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import { Navigate, Outlet } from 'react-router';
import { useAuth } from '@/shared/hooks/use-auth';

export function AuthGuard() {
  const { t } = useTranslation();
  const { isAuthenticated, isLoading, profile, authError, retryAuth } = useAuth();

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spinner size="32px" color="accent" />
      </div>
    );
  }

  if (!isAuthenticated) {
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
        <div style={{ marginBottom: 8 }}>
          <Text type="title2" weight="bold">
            {t('auth.guard.title')}
          </Text>
        </div>
        <div style={{ marginBottom: 24, maxWidth: 320 }}>
          <Text type="subheadline2" color="secondary">
            {t('auth.guard.description')}
          </Text>
        </div>

        <div style={{ width: '100%', maxWidth: 240 }}>
          <Button text={t('auth.guard.retry')} type="primary" onClick={() => retryAuth()} />
        </div>

        {import.meta.env.DEV && authError ? (
          <div style={{ marginTop: 16, maxWidth: 320 }}>
            <Text type="caption1" color="secondary">
              {authError.message}
            </Text>
          </div>
        ) : null}
      </div>
    );
  }

  if (profile && !profile.onboardingCompleted) {
    return <Navigate to="/onboarding" replace />;
  }

  return <Outlet />;
}
