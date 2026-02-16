import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { themeParams, useSignal } from '@telegram-apps/sdk-react';
import { Spinner, ThemeProvider, ToastProvider } from '@telegram-tools/ui-kit';
import { MotionConfig } from 'motion/react';
import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from 'react-router';
import { AuthGuard, ErrorBoundary } from '@/shared/ui';
import { DeepLinkHandler } from './deep-link-handler';
import { OnboardingLayout } from './layouts/onboarding-layout';
import { TabLayout } from './layouts/tab-layout';

function lazyRetry<T extends { default: React.ComponentType }>(
  factory: () => Promise<T>,
  retries = 2,
): React.LazyExoticComponent<T['default']> {
  return lazy(() =>
    factory().catch((err: unknown) => {
      if (retries > 0) {
        return new Promise<T>((resolve) => setTimeout(resolve, 500)).then(
          () => lazyRetry(factory, retries - 1) as unknown as T,
        );
      }
      // After retries exhausted, force reload to get fresh assets.
      const reloaded = sessionStorage.getItem('chunk_reload');
      if (!reloaded) {
        sessionStorage.setItem('chunk_reload', '1');
        window.location.reload();
      }
      throw err;
    }),
  );
}

const OnboardingPage = lazyRetry(() => import('@/pages/onboarding/OnboardingPage'));
const OnboardingInterestPage = lazyRetry(() => import('@/pages/onboarding/OnboardingInterestPage'));
const OnboardingTourPage = lazyRetry(() => import('@/pages/onboarding/OnboardingTourPage'));

const CatalogPage = lazyRetry(() => import('@/pages/catalog/CatalogPage'));
const ChannelDetailPage = lazyRetry(() => import('@/pages/catalog/ChannelDetailPage'));
const DealsPage = lazyRetry(() => import('@/pages/deals/DealsPage'));
const CreateDealPage = lazyRetry(() => import('@/pages/deals/CreateDealPage'));
const DealDetailPage = lazyRetry(() => import('@/pages/deals/DealDetailPage'));
const WalletPage = lazyRetry(() => import('@/pages/wallet/WalletPage'));
const HistoryPage = lazyRetry(() => import('@/pages/wallet/HistoryPage'));
const TransactionDetailPage = lazyRetry(() => import('@/pages/wallet/TransactionDetailPage'));
const ProfilePage = lazyRetry(() => import('@/pages/profile/ProfilePage'));
const LocaleCurrencyPage = lazyRetry(() => import('@/pages/profile/LocaleCurrencyPage'));
const NotificationsPage = lazyRetry(() => import('@/pages/profile/NotificationsPage'));
const RegisterChannelPage = lazyRetry(() => import('@/pages/profile/RegisterChannelPage'));
const CreativesPage = lazyRetry(() => import('@/pages/creatives/CreativesPage'));
const CreativeEditorPage = lazyRetry(() => import('@/pages/creatives/CreativeEditorPage'));

const TonConnectLayout = lazyRetry(() =>
  import('@/shared/ton/TonConnectProvider').then((m) => ({ default: m.TonConnectProvider })),
);

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function useTelegramTheme(): 'light' | 'dark' {
  // Force theme for dev/E2E matrix runs outside Telegram.
  const forced = import.meta.env.VITE_FORCE_THEME;
  if (forced === 'dark' || forced === 'light') return forced;

  // Telegram-native theme sync (signal updates on theme change).
  const isDark = useSignal(themeParams.isDark, () => false);
  if (isDark) return 'dark';

  // Outside Telegram: detect system color scheme preference.
  const isMounted = themeParams.isMounted();
  if (!isMounted && typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark';
  }

  return 'light';
}

function ErrorBoundaryLayout() {
  const location = useLocation();
  return (
    <ErrorBoundary resetKey={location.pathname}>
      <Outlet />
    </ErrorBoundary>
  );
}

function AppRoutes() {
  const location = useLocation();

  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/onboarding" element={<OnboardingLayout />}>
          <Route index element={<OnboardingPage />} />
          <Route path="interest" element={<OnboardingInterestPage />} />
          <Route path="tour" element={<OnboardingTourPage />} />
        </Route>

        <Route element={<AuthGuard />}>
          <Route
            element={
              <ErrorBoundary resetKey={location.pathname}>
                <TabLayout />
              </ErrorBoundary>
            }
          >
            <Route path="/catalog" element={<CatalogPage />} />
            <Route path="/deals" element={<DealsPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            {/* Wallet tab — lazy TonConnect provider keeps TON chunk out of initial load */}
            <Route element={<TonConnectLayout />}>
              <Route path="/wallet" element={<WalletPage />} />
            </Route>
          </Route>

          <Route element={<ErrorBoundaryLayout />}>
            <Route path="/catalog/channels/:channelId" element={<ChannelDetailPage />} />
            <Route path="/wallet/history" element={<HistoryPage />} />
            <Route path="/wallet/history/:txId" element={<TransactionDetailPage />} />
            <Route path="/profile/locale-currency" element={<LocaleCurrencyPage />} />
            <Route path="/profile/language" element={<Navigate to="/profile/locale-currency" replace />} />
            <Route path="/profile/currency" element={<Navigate to="/profile/locale-currency" replace />} />
            <Route path="/profile/notifications" element={<NotificationsPage />} />
            <Route path="/profile/channels/new" element={<RegisterChannelPage />} />
            <Route path="/profile/creatives" element={<CreativesPage />} />
            <Route path="/profile/creatives/new" element={<CreativeEditorPage />} />
            <Route path="/profile/creatives/:creativeId/edit" element={<CreativeEditorPage />} />
          </Route>

          {/* Deal detail/create — lazy TonConnect for PaymentSheet */}
          <Route element={<TonConnectLayout />}>
            <Route element={<ErrorBoundaryLayout />}>
              <Route path="/deals/:dealId" element={<DealDetailPage />} />
              <Route path="/deals/new" element={<CreateDealPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/catalog" replace />} />
      </Routes>
    </Suspense>
  );
}

export function App() {
  const theme = useTelegramTheme();

  return (
    <MotionConfig reducedMotion="user">
      <ThemeProvider theme={theme}>
        <ToastProvider>
          <QueryClientProvider client={queryClient}>
            <ErrorBoundary>
              <BrowserRouter>
                <DeepLinkHandler />
                <AppRoutes />
              </BrowserRouter>
            </ErrorBoundary>
          </QueryClientProvider>
        </ToastProvider>
      </ThemeProvider>
    </MotionConfig>
  );
}

function PageLoader() {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: 'var(--am-viewport-stable-height)',
      }}
    >
      <Spinner size="32px" color="accent" />
    </div>
  );
}
