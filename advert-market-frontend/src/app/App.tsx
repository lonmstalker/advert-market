import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Spinner, ThemeProvider, ToastProvider } from '@telegram-tools/ui-kit';
import { TonConnectUIProvider } from '@tonconnect/ui-react';
import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import { AuthGuard } from '@/shared/ui';
import { DeepLinkHandler } from './deep-link-handler';
import { OnboardingLayout } from './layouts/onboarding-layout';
import { TabLayout } from './layouts/tab-layout';

const OnboardingPage = lazy(() => import('@/pages/onboarding/OnboardingPage'));
const OnboardingInterestPage = lazy(() => import('@/pages/onboarding/OnboardingInterestPage'));
const OnboardingTourPage = lazy(() => import('@/pages/onboarding/OnboardingTourPage'));

const CatalogPage = lazy(() => import('@/pages/catalog/CatalogPage'));
const DealsPage = lazy(() => import('@/pages/deals/DealsPage'));
const WalletPage = lazy(() => import('@/pages/wallet/WalletPage'));
const ProfilePage = lazy(() => import('@/pages/profile/ProfilePage'));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const TON_MANIFEST_URL = `${window.location.origin}/tonconnect-manifest.json`;

function useTelegramTheme(): 'light' | 'dark' {
  try {
    const colorScheme = window.Telegram?.WebApp?.colorScheme;
    return colorScheme === 'dark' ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

export function App() {
  const theme = useTelegramTheme();

  return (
    <TonConnectUIProvider manifestUrl={TON_MANIFEST_URL}>
      <ThemeProvider theme={theme}>
        <ToastProvider>
          <QueryClientProvider client={queryClient}>
            <BrowserRouter>
              <DeepLinkHandler />
              <Suspense fallback={<PageLoader />}>
                <Routes>
                  <Route path="/onboarding" element={<OnboardingLayout />}>
                    <Route index element={<OnboardingPage />} />
                    <Route path="interest" element={<OnboardingInterestPage />} />
                    <Route path="tour" element={<OnboardingTourPage />} />
                  </Route>

                  <Route element={<AuthGuard />}>
                    <Route element={<TabLayout />}>
                      <Route path="/catalog" element={<CatalogPage />} />
                      <Route path="/deals" element={<DealsPage />} />
                      <Route path="/wallet" element={<WalletPage />} />
                      <Route path="/profile" element={<ProfilePage />} />
                    </Route>
                  </Route>

                  <Route path="*" element={<Navigate to="/catalog" replace />} />
                </Routes>
              </Suspense>
            </BrowserRouter>
          </QueryClientProvider>
        </ToastProvider>
      </ThemeProvider>
    </TonConnectUIProvider>
  );
}

function PageLoader() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <Spinner size="32px" color="accent" />
    </div>
  );
}
