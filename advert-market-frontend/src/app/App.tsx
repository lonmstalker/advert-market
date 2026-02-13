import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Spinner, ThemeProvider, ToastProvider } from '@telegram-tools/ui-kit';
import { TonConnectUIProvider } from '@tonconnect/ui-react';
import { MotionConfig } from 'motion/react';
import { lazy, Suspense, useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import { AuthGuard, ErrorBoundary } from '@/shared/ui';
import { DeepLinkHandler } from './deep-link-handler';
import { OnboardingLayout } from './layouts/onboarding-layout';
import { TabLayout } from './layouts/tab-layout';

const OnboardingPage = lazy(() => import('@/pages/onboarding/OnboardingPage'));
const OnboardingInterestPage = lazy(() => import('@/pages/onboarding/OnboardingInterestPage'));
const OnboardingTourPage = lazy(() => import('@/pages/onboarding/OnboardingTourPage'));

const CatalogPage = lazy(() => import('@/pages/catalog/CatalogPage'));
const ChannelDetailPage = lazy(() => import('@/pages/catalog/ChannelDetailPage'));
const DealsPage = lazy(() => import('@/pages/deals/DealsPage'));
const CreateDealPage = lazy(() => import('@/pages/deals/CreateDealPage'));
const DealDetailPage = lazy(() => import('@/pages/deals/DealDetailPage'));
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

function getTheme(): 'light' | 'dark' {
  try {
    return window.Telegram?.WebApp?.colorScheme === 'dark' ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

function useTelegramTheme(): 'light' | 'dark' {
  const [theme, setTheme] = useState(getTheme);

  useEffect(() => {
    const webApp = window.Telegram?.WebApp;
    if (!webApp?.onEvent) return;

    const handler = () => setTheme(getTheme());
    webApp.onEvent('themeChanged', handler);
    return () => webApp.offEvent('themeChanged', handler);
  }, []);

  return theme;
}

export function App() {
  const theme = useTelegramTheme();

  return (
    <MotionConfig reducedMotion="user">
      <TonConnectUIProvider manifestUrl={TON_MANIFEST_URL}>
        <ThemeProvider theme={theme}>
          <ToastProvider>
            <QueryClientProvider client={queryClient}>
              <ErrorBoundary>
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
                        <Route path="/catalog/channels/:channelId" element={<ChannelDetailPage />} />
                        <Route path="/deals/:dealId" element={<DealDetailPage />} />
                        <Route path="/deals/new" element={<CreateDealPage />} />
                      </Route>

                      <Route path="*" element={<Navigate to="/catalog" replace />} />
                    </Routes>
                  </Suspense>
                </BrowserRouter>
              </ErrorBoundary>
            </QueryClientProvider>
          </ToastProvider>
        </ThemeProvider>
      </TonConnectUIProvider>
    </MotionConfig>
  );
}

function PageLoader() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <Spinner size="32px" color="accent" />
    </div>
  );
}
