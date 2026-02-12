import { Spinner } from '@telegram-tools/ui-kit';
import { Navigate, Outlet } from 'react-router';
import { useAuth } from '@/features/auth';

export function AuthGuard() {
  const { isAuthenticated, isLoading, profile } = useAuth();

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spinner size="32px" color="accent" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spinner size="32px" color="accent" />
      </div>
    );
  }

  if (profile && !profile.onboardingCompleted) {
    return <Navigate to="/onboarding" replace />;
  }

  return <Outlet />;
}
